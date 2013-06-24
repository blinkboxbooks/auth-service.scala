require "sinatra/base"
require "active_record"
require "multi_json"
require "sandal"
require "scrypt"

require "sinatra/request-bearer_token"
require "sinatra/authorization"
require "sinatra/oauth_helper"

require "blinkbox/zuul/server/environment"
require "blinkbox/zuul/server/access_token"
require "blinkbox/zuul/server/client"
require "blinkbox/zuul/server/refresh_token"
require "blinkbox/zuul/server/user"

module Blinkbox
  module Zuul
    class Server < Sinatra::Base
      helpers Sinatra::OAuthHelper
      register Sinatra::Authorization

      ACCESS_TOKEN_LIFETIME = 1800
      REFRESH_TOKEN_LIFETIME = 3600 * 24 * 90
      REFRESH_TOKEN_REISSUE_INTERVAL = 3600 * 24 * 7
      MIN_PASSWORD_LENGTH = 6

      require_user_authorization_for "/oauth2/client"
      require_client_authorization_for %r{/oauth2/client/(\d+)(?:/.*)?}

      post "/oauth2/client" do
        begin
          data = MultiJson.load(request.body.read)
        rescue MultiJson::LoadError
          invalid_request "The request body is not valid JSON"
        end

        client = Client.new do |c|
          c.name = data["client_name"] || "Unknown Client (#{Time.now.strftime("%d %b %Y")})"
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

      get "/oauth2/token" do # discouraged, but required by the spec
        handle_token_request(params)
      end

      post "/oauth2/token" do
        handle_token_request(params)
      end

      private

      def return_client_information(client)
        json({
          "client_id" => "urn:blinkboxbooks:id:client:#{client.id}",
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
        when "urn:blinkboxbooks:oauth:grant-type:registration"
          handle_registration_flow(params)
        when "password"
          handle_password_flow(params)
        when "refresh_token"
          handle_refresh_token_flow(params)
        else
          invalid_request "The grant type '#{params[:grant_type]}' is not supported"
        end
      end

      def handle_registration_flow(params)
        first_name, last_name, email, password = params[:first_name], params[:last_name], params[:username], params[:password]
        if password.nil? || password.length < MIN_PASSWORD_LENGTH
          invalid_request "Validation failed: Password is too short (minimum is #{MIN_PASSWORD_LENGTH} characters)"
        end
        
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
        if email.nil? || password.nil?
          invalid_request "The 'username' and 'password' parameters are required for the 'password' grant type"
        end

        user = User.authenticate(email, password)
        invalid_grant "The username and/or password is incorrect." if user.nil?

        client_id, client_secret = params[:client_id], params[:client_secret]
        unless client_id.nil?
          client = Client.authenticate(client_id, client_secret)
          invalid_client "The client id and/or client secret is incorrect." if client.nil?
          invalid_client "You are not authorised to use this client." unless client.user == user
        end

        issue_refresh_token(user, client)
      end

      def handle_refresh_token_flow(params)
        token_value = params[:refresh_token]
        if token_value.nil?
          invalid_request "The 'refresh_token' parameter is required for the 'refresh_token' grant type"
        end

        refresh_token = RefreshToken.find_by_token(token_value)
        if refresh_token.nil?
          invalid_grant "The refresh token is invalid"
        elsif refresh_token.expires_at < Time.now
          invalid_grant "The refresh token has expired"
        elsif refresh_token.revoked
          invalid_grant "The refresh token has been revoked"
        end

        client_id, client_secret = params[:client_id], params[:client_secret]
        unless client_id.nil?
          client = Client.authenticate(client_id, client_secret)
          invalid_client "The client id and/or client secret is incorrect." if client.nil?
          invalid_client "Your client is not authorised to use this refresh token." unless client.user == refresh_token.user
        end
        if refresh_token.client.nil?
          refresh_token.client = client
        elsif refresh_token.client != client
          invalid_client "Your client is not authorised to use this refresh token."
        end

        refresh_token.expires_at = Time.now + REFRESH_TOKEN_LIFETIME
        issue_access_token(refresh_token)
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
        refresh_token.access_token = AccessToken.new do |token|
          token.expires_at = Time.now + ACCESS_TOKEN_LIFETIME
        end
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
          "sub" => "urn:blinkboxbooks:id:user:#{refresh_token.user.id}",
          "iat" => access_token.created_at.to_i,
          "exp" => access_token.expires_at.to_i,
          "jti" => "urn:blinkboxbooks:oauth:access-token:#{access_token.id}",
          "bbb/uid" => refresh_token.user.id,
        }
        claims["bbb/cid"] = "urn:blinkboxbooks:id:client:#{refresh_token.client.id}" if refresh_token.client

        signer = Sandal::Sig::ES256.new(File.read("./keys/auth_server_ec_priv.pem"))
        jws_token = Sandal.encode_token(claims, signer, { "kid" => "/bbb/auth/sig/ec/1" })

        # TODO: Uncomment to use an RSA signed token instead of an ECDSA one
        # signer = Sandal::Sig::RS256.new(File.read("./keys/auth_server_rsa_priv.pem"))
        # jws_token = Sandal.encode_token(claims, signer, { kid: "/bbb/auth/sig/rsa/1" })

        encrypter = Sandal::Enc::A128GCM.new(Sandal::Enc::Alg::Direct.new(File.read("./keys/shared_aes128.key")))
        Sandal.encrypt_token(jws_token, encrypter, { "kid" => "/bbb/svcs/enc/a128/1", "cty" => "JWT" })
      end

    end
  end
end