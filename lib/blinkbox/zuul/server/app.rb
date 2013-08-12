require "multi_json"
require "sandal"
require "scrypt"

require "rack/blinkbox/zuul/tokens"
require "sinatra/json_helper"
require "sinatra/oauth_helper"
require "sinatra/blinkbox/zuul/authorization"
require "blinkbox/zuul/server/environment"

module Blinkbox::Zuul::Server
  class App < Sinatra::Base
    use Rack::Blinkbox::Zuul::TokenDecoder
    helpers Sinatra::JSONHelper
    helpers Sinatra::OAuthHelper
    register Sinatra::Blinkbox::Zuul::Authorization

    require_user_authorization_for %r{^/clients(?:/.*)?}
    require_user_authorization_for %r{^/users(?:/.*)?}

    after do
      # none of the responses from the auth server should be cached
      cache_control :no_store
      response["Date"] = response["Expires"] = Time.now.rfc822.to_s
      response["Pragma"] = "no-cache"
    end

    post "/clients", provides: :json do
      if current_user.clients.count >= Client::MAX_CLIENTS_PER_USER
        invalid_request "client_limit_reached", "Max clients (#{Client::MAX_CLIENTS_PER_USER}) already registered"
      end

      client = Client.new do |c|
        c.name = params["client_name"] || "Unknown Client"
        c.user = current_user
        c.client_secret = generate_opaque_token
      end

      begin
        client.save!
      rescue => e
        invalid_request e.message
      end

      json build_client_info(client, include_client_secret: true)
    end

    get "/clients", provides: :json do
      client_infos = current_user.clients.map { |client| build_client_info(client) }
      json({ "clients" => client_infos })
    end

    get "/clients/:client_id", provides: :json do |client_id|
      client = Client.find_by_id(client_id)
      halt 404 if client.nil? || client.user != current_user
      json build_client_info(client)
    end

    post "/clients/:client_id", provides: :json do |client_id|
      client = Client.find_by_id(client_id)
      halt 404 if client.nil? || client.user != current_user

      updateable = ["name"]
      updates = params.select { |k, v| updateable.include?(k) }
      begin
        client.update_attributes!(updates)
      rescue => e
        invalid_request e.message
      end

      json build_client_info(client)
    end

    get "/oauth2/token", provides: :json do
      handle_token_request(params)
    end

    post "/oauth2/token", provides: :json do
      handle_token_request(params)
    end

    get "/users/:user_id", provides: :json do |user_id|
      halt 404 unless user_id == current_user.id.to_s
      json build_user_info(current_user)
    end

    post "/users/:user_id", provides: :json do |user_id|
      halt 404 unless user_id == current_user.id.to_s
      invalid_request "Cannot change acceptance of terms and conditions" if params["accepted_terms_and_conditions"]

      updateable = ["username", "first_name", "last_name", "allow_marketing_communications"]
      updates = params.select { |k, v| updateable.include?(k) }
      begin
        current_user.update_attributes!(updates)
      rescue => e
        invalid_request e.message
      end

      json build_user_info(current_user)
    end

    private

    def handle_token_request(params)
      case params["grant_type"]
      when "password"
        handle_password_flow(params)
      when "refresh_token"
        handle_refresh_token_flow(params)
      when "urn:blinkbox:oauth:grant-type:registration"
        handle_registration_flow(params)
      else
        invalid_request "The grant type '#{params["grant_type"]}' is not supported"
      end
    end

    def handle_registration_flow(params)
      invalid_request "You must accept the terms and conditions" unless params["accepted_terms_and_conditions"] == "true"

      user = User.new do |u|
        u.first_name = params["first_name"]
        u.last_name = params["last_name"]
        u.username = params["username"]
        u.password = params["password"]
        u.allow_marketing_communications = params["allow_marketing_communications"]
      end

      begin
        user.save!
      rescue ActiveRecord::RecordInvalid => e
        if user.errors[:username].include?(user.errors.generate_message(:username, :taken)) 
          invalid_request "username_already_taken", e.message
        else
          invalid_request e.message
        end
      end

      issue_refresh_token(user)
    end

    def handle_password_flow(params)
      username, password = params["username"], params["password"]
      invalid_request "The username and password are required for this grant type" if username.nil? || password.nil?

      user = User.authenticate(username, password)
      invalid_grant "The username and/or password is incorrect." if user.nil?

      client = authenticate_client(params, user)
      issue_refresh_token(user, client)
    end

    def handle_refresh_token_flow(params)
      token_value = params["refresh_token"]
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

      refresh_token.extend_lifetime
      issue_access_token(refresh_token)
    end

    def authenticate_client(params, user)
      client_id, client_secret = params["client_id"], params["client_secret"]
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
      end
      issue_access_token(refresh_token, include_refresh_token: true)
    end

    def issue_access_token(refresh_token, include_refresh_token = false)
      refresh_token.access_token = AccessToken.new
      refresh_token.save!

      token_info = {
        "access_token" => build_access_token(refresh_token),
        "token_type" => "bearer",
        "expires_in" => AccessToken::LIFETIME_IN_SECONDS
      }
      token_info["refresh_token"] = refresh_token.token if include_refresh_token
      json token_info.merge(build_user_info(refresh_token.user, format: :basic))
    end

    def build_client_info(client, include_client_secret = false)
      client_info = {
        "client_id" => "urn:blinkbox:zuul:client:#{client.id}",
        "client_uri" => "#{base_url}/clients/#{client.id}",
        "client_name" => client.name
      }
      client_info["client_secret"] = client.client_secret if include_client_secret
      client_info
    end

    def build_user_info(user, format = :complete)
      user_info = {
        "user_id" => "urn:blinkbox:zuul:user:#{user.id}",
        "user_uri" => "#{base_url}/users/#{user.id}",
        "user_username" => user.username,
        "user_first_name" => user.first_name,
        "user_last_name" => user.last_name        
      }
      if format == :complete
        user_info["user_allow_marketing_communications"] = user.allow_marketing_communications
      end
      user_info
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