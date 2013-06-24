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

    require_user_authorization_for "/oauth2/client"
    require_client_authorization_for %r{/oauth2/client/(\d+)(?:/.*)?}

    post "/oauth2/client" do
      data = request_body_json
      client = Client.new do |c|
        c.name = data["client_name"] || "Unknown Client"
        c.user = current_user
        c.client_secret = Sandal::Util.jwt_base64_encode(SecureRandom.random_bytes(32))
        c.registration_access_token = Sandal::Util.jwt_base64_encode(SecureRandom.random_bytes(32))
      end
      client.save!
      return_client_information(client)
    end

    get %r{/oauth2/client/(\d+)} do
      return_client_information(current_client)
    end

    get "/oauth2/token" do
      handle_token_request(params)
    end

    post "/oauth2/token" do
      handle_token_request(params)
    end

    private

    def request_body_json
      MultiJson.load(request.body.read)
    rescue MultiJson::LoadError
      invalid_request "The request body is not valid JSON"
    end

    def return_client_information(client)
      json({
        "client_id" => "urn:blinkbox:zuul:client:#{client.id}",
        "client_id_issued_at" => client.created_at.to_i,
        "client_secret" => client.client_secret,
        "client_secret_expires_at" => 0,
        "registration_access_token" => client.registration_access_token,
        "registration_client_uri" => "#{base_url}/oauth2/client/#{client.id}",
        "client_name" => client.name
      })
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
      first_name, last_name, email, password = params[:first_name], params[:last_name], params[:username], params[:password]
      user = User.new(first_name: first_name, last_name: last_name, email: email, password: password)
      begin
        user.save!
      rescue ActiveRecord::RecordInvalid => e
        invalid_request e.message
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
      invalid_grant "The refresh token has expired" if refresh_token.expires_at < Time.now
      invalid_grant "The refresh token has been revoked" if refresh_token.revoked

      client = authenticate_client(params, refresh_token.user)
      if refresh_token.client.nil?
        refresh_token.client = client
      elsif refresh_token.client != client
        invalid_client "Your client is not authorised to use this refresh token."
      end

      refresh_token.expires_at = Time.now + REFRESH_TOKEN_LIFETIME
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
      refresh_token = RefreshToken.new do |token|
        token.user = user
        token.client = client
        token.token = Sandal::Util.jwt_base64_encode(SecureRandom.random_bytes(32))
        token.expires_at = Time.now + REFRESH_TOKEN_LIFETIME
      end
      issue_access_token(refresh_token, include_refresh_token: true)
    end

    def issue_access_token(refresh_token, include_refresh_token = false)
      refresh_token.access_token = AccessToken.new(expires_at: Time.now + ACCESS_TOKEN_LIFETIME)
      refresh_token.save!

      response_body = {
        "access_token" => build_access_token(refresh_token),
        "token_type" => "bearer",
        "expires_in" => ACCESS_TOKEN_LIFETIME
      }
      response_body["refresh_token"] = refresh_token.token if include_refresh_token
      json response_body
    end

    def build_access_token(refresh_token)
      access_token = refresh_token.access_token
      claims = {
        "sub" => "urn:blinkbox:zuul:user:#{refresh_token.user.id}",
        "iat" => access_token.created_at.to_i,
        "exp" => access_token.expires_at.to_i,
        "jti" => "urn:blinkbox:zuul:access-token:#{access_token.id}"
      }
      claims["bb/cid"] = "urn:blinkbox:zuul:client:#{refresh_token.client.id}" if refresh_token.client

      sig_key_id = "blinkbox/zuul/sig/ec/1" # TODO: This should be a setting
      signer = Sandal::Sig::ES256.new(File.read("./keys/#{sig_key_id}/private.pem"))
      jws_token = Sandal.encode_token(claims, signer, { "kid" => sig_key_id })
      
      enc_key_id = "blinkbox/plat/enc/rsa/1" # TODO: This should be a setting
      encrypter = Sandal::Enc::A128GCM.new(Sandal::Enc::Alg::RSA_OAEP.new(File.read("./keys/#{enc_key_id}/public.pem")))
      Sandal.encrypt_token(jws_token, encrypter, { "kid" => enc_key_id, "cty" => "JWT" })
    end
  end
end