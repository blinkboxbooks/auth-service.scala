require "multi_json"
require "sandal"
require "scrypt"

require "blinkbox/zuul/server/environment"
require "sinatra/request-bearer_token"
require "sinatra/authorization"
require "sinatra/json"
require "sinatra/oauth_helper"

module Blinkbox::Zuul::Server
  class App < Sinatra::Base
    helpers Sinatra::JSON
    helpers Sinatra::OAuthHelper
    register Sinatra::Authorization

    ACCESS_TOKEN_LIFETIME = 1800
    REFRESH_TOKEN_LIFETIME = 3600 * 24 * 90
    MAX_CLIENTS_PER_USER = 12

    require_user_authorization_for %r{/clients(?:/.*)?}

    post "/clients", provides: :json do
      if current_user.clients.count >= MAX_CLIENTS_PER_USER
        halt 403, json({ 
          "error" => "client_limit_reached", 
          "error_description" => "The allowed number of clients (#{MAX_CLIENTS_PER_USER}) are already registered" 
        })
      end

      client = Client.new do |c|
        c.name = params[:client_name] || "Unknown Client"
        c.user = current_user
        c.client_secret = generate_opaque_token
      end
      client.save!
      return_client_information(client, include_client_secret: true)
    end

    get "/clients/:client_id", provides: :json do |client_id|
      client = Client.find_by_id(client_id)
      invalid_client "You are not authorised to use this client." unless client.user == current_user
      return_client_information(client)
    end

    get "/oauth2/token", provides: :json do
      handle_token_request(params)
    end

    post "/oauth2/token", provides: :json do
      handle_token_request(params)
    end

    private

    def return_client_information(client, include_client_secret = false)
      client_info = {
        "client_id" => "urn:blinkbox:zuul:client:#{client.id}",
        "client_name" => client.name,
        "client_uri" => "#{base_url}/clients/#{client.id}"
      }
      client_info["client_secret"] = client.client_secret if include_client_secret
      json client_info
    end

    def handle_token_request(params)
      case params[:grant_type]
      when "password"
        handle_password_flow(params)
      when "refresh_token"
        handle_refresh_token_flow(params)
      when "urn:blinkbox:oauth:grant-type:registration"
        handle_registration_flow(params)
      else
        invalid_request "The grant type '#{params[:grant_type]}' is not supported"
      end
    end

    def handle_registration_flow(params)
      invalid_request "You must accept the terms and conditions" unless params[:accepted_terms_and_conditions] == "true"

      user = User.new do |u|
        u.first_name = params[:first_name]
        u.last_name = params[:last_name]
        u.email = params[:username]
        u.password = params[:password]
        u.allow_marketing_communications = params[:allow_marketing_communications]
      end

      begin
        user.save!
      rescue ActiveRecord::RecordInvalid => e
        if user.errors[:email].include?(user.errors.generate_message(:email, :taken)) 
          invalid_request "username_already_taken", e.message
        else
          invalid_request e.message
        end
      end

      issue_refresh_token(user)
    end

    def handle_password_flow(params)
      email, password = params[:username], params[:password]
      invalid_request "The username and password are required for this grant type" if email.nil? || password.nil?

      user = User.authenticate(email, password)
      invalid_grant "The username and/or password is incorrect." if user.nil?

      client = authenticate_client(params, user)
      issue_refresh_token(user, client)
    end

    def handle_refresh_token_flow(params)
      token_value = params[:refresh_token]
      invalid_request "The refresh token is required for this grant type" if token_value.nil?

      refresh_token = RefreshToken.find_by_token(token_value)
      invalid_grant "The refresh token is invalid" if refresh_token.nil?
      invalid_grant "The refresh token has expired" if refresh_token.expires_at < DateTime.now
      invalid_grant "The refresh token has been revoked" if refresh_token.revoked

      client = authenticate_client(params, refresh_token.user)
      if refresh_token.client.nil?
        refresh_token.client = client
      elsif refresh_token.client != client
        invalid_client "Your client is not authorised to use this refresh token."
      end

      refresh_token.expires_at = DateTime.now + REFRESH_TOKEN_LIFETIME
      issue_access_token(refresh_token)
    end

    def authenticate_client(params, user)
      client_id, client_secret = params[:client_id], params[:client_secret]
      unless client_id.nil?
        client = Client.authenticate(client_id, client_secret)
        invalid_client "The client id and/or client secret is incorrect." if client.nil?
        invalid_client "You are not authorised to use this client." unless client.user == user
      end
      client
    end

    def issue_refresh_token(user, client = nil)
      # note: this method and issue_access_token are effectively a pair of methods; this one always
      # calls that one to save the refresh token which means this one doesn't need to do it. it's
      # a little bit dirty i guess, but there's no point in saving the refresh token twice.
      refresh_token = RefreshToken.new do |rt|
        rt.user = user
        rt.client = client
        rt.token = generate_opaque_token
        rt.expires_at = Time.now + REFRESH_TOKEN_LIFETIME
      end
      issue_access_token(refresh_token, include_refresh_token: true)
    end

    def issue_access_token(refresh_token, include_refresh_token = false)
      refresh_token.access_token = AccessToken.new(expires_at: Time.now + ACCESS_TOKEN_LIFETIME)
      refresh_token.save!

      token_info = {
        "access_token" => build_access_token(refresh_token),
        "token_type" => "bearer",
        "expires_in" => ACCESS_TOKEN_LIFETIME
      }
      token_info["refresh_token"] = refresh_token.token if include_refresh_token
      json token_info
    end

    def build_access_token(refresh_token)
      access_token = refresh_token.access_token
      claims = {
        "sub" => "urn:blinkbox:zuul:user:#{refresh_token.user.id}",
        "exp" => access_token.expires_at.to_i,
        "jti" => "urn:blinkbox:zuul:access-token:#{access_token.id}"
      }
      claims["bb/cid"] = "urn:blinkbox:zuul:client:#{refresh_token.client.id}" if refresh_token.client

      sig_key_id = settings.properties["signing_key_id"]
      signer = Sandal::Sig::ES256.new(File.read("./keys/#{sig_key_id}/private.pem"))
      jws_token = Sandal.encode_token(claims, signer, { "kid" => sig_key_id })
      
      enc_key_id = settings.properties["encryption_key_id"]
      encrypter = Sandal::Enc::A128GCM.new(Sandal::Enc::Alg::RSA_OAEP.new(File.read("./keys/#{enc_key_id}/public.pem")))
      Sandal.encrypt_token(jws_token, encrypter, { "kid" => enc_key_id, "cty" => "JWT" })
    end

    def generate_opaque_token
      Sandal::Util.jwt_base64_encode(SecureRandom.random_bytes(32))
    end

  end
end