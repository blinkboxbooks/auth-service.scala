require "ipaddress"
require "ipaddress/ipv4_loopback"
require "multi_json"
require "sandal"
require "scrypt"

require "rack/blinkbox/zuul/tokens"
require "rack/jsonlogger"
require "rack/timekeeper"
require "sinatra/contrib"
require "sinatra/json_helper"
require "sinatra/oauth_helper"
require "sinatra/www_authenticate_helper"
require "sinatra/blinkbox/zuul/authorization"
require "sinatra/blinkbox/zuul/elevation"
require "blinkbox/zuul/server/environment"
require "blinkbox/zuul/server/errors"
require "blinkbox/zuul/server/email"
require "blinkbox/zuul/server/reporting"

module Blinkbox::Zuul::Server
  class App < Sinatra::Base
    LOGGER_NAME = "zuul.errors"

    use Rack::Timekeeper, logdev: settings.properties["logging.perf.file"], level: ::Logger.const_get(settings.properties["logging.perf.level"]) do |duration|
      if duration < settings.properties["logging.perf.threshold.info"].to_i then :DEBUG
      elsif duration < settings.properties["logging.perf.threshold.warn"].to_i then :INFO
      elsif duration < settings.properties["logging.perf.threshold.error"].to_i then :WARN
      else :ERROR
      end
    end
    use Rack::JsonLogger, LOGGER_NAME, logdev: settings.properties["logging.error.file"], level: ::Logger.const_get(settings.properties["logging.error.level"])
    use Rack::Blinkbox::Zuul::TokenDecoder
    helpers Sinatra::JSONHelper
    helpers Sinatra::OAuthHelper
    helpers Sinatra::WWWAuthenticateHelper
    register Sinatra::Namespace
    register Sinatra::Blinkbox::Zuul::Authorization
    register Sinatra::Blinkbox::Zuul::Elevation

    require_user_authorization_for %r{^/clients}
    require_user_authorization_for %r{^/users}
    require_user_authorization_for %r{^/password/change}
    require_user_authorization_for %r{^/session}

    require_elevation_for %r{^/users}
    require_elevation_for %r{^/clients}, methods: %i(post patch delete)
    require_elevation_for %r{^/session}, level: :elevated, methods: :post

    after do
      # none of the responses from the auth server should be cached
      cache_control :no_store
      response["Date"] = response["Expires"] = Time.now.rfc822.to_s
      response["Pragma"] = "no-cache"
      response['X-Application-Version'] = VERSION
    end

    error TooManyRequests do
      env[LOGGER_NAME].warn("Requests are being throttled: #{env["sinatra.error"].message}")
      halt 429, { "Retry-After" => env["sinatra.error"].retry_after.ceil.to_s }, nil
    end

    error do
      env[LOGGER_NAME].error(env["sinatra.error"])
      halt 500, nil
    end

    post "/clients", provides: :json do
      client = register_client()
      json build_client_info(client, include_client_secret: true)
    end

    get "/clients", provides: :json do
      client_infos = current_user.registered_clients.map { |client| build_client_info(client) }
      json({ "clients" => client_infos })
    end

    get "/clients/:client_id", provides: :json do |client_id|
      client = Client.find_by_id(client_id)
      halt 404 if client.nil? || client.user != current_user || client.deregistered
      json build_client_info(client)
    end

    patch "/clients/:client_id", provides: :json do |client_id|
      client = Client.find_by_id(client_id)
      halt 404 if client.nil? || client.user != current_user || client.deregistered

      updates = {}
      %w{name brand model os}.each do |key|
        updates[key] = params["client_#{key}"] if params["client_#{key}"]
      end
      invalid_request "No updateable attributes specified" if updates.empty?

      begin
        client.update_attributes!(updates)
      rescue => e
        invalid_request e.message
      end

      json build_client_info(client)
    end

    delete "/clients/:client_id", provides: :json do |client_id|
      client = Client.find_by_id(client_id)
      halt 404 if client.nil? || client.user != current_user || client.deregistered

      client.deregister
    end

    get "/oauth2/token", provides: :json do
      handle_token_request(params)
    end

    post "/oauth2/token", provides: :json do
      handle_token_request(params)
    end

    post "/tokens/revoke" do
      token_value = params["refresh_token"]
      invalid_request "The refresh token is required for this grant type" if token_value.nil?

      refresh_token = RefreshToken.find_by_token(token_value)
      invalid_grant "The refresh token is invalid" if refresh_token.nil?

      refresh_token.revoked = true
      refresh_token.save!
      nil # no entity-body needed
    end

    get "/users/:user_id", provides: :json do |user_id|
      halt 404 unless user_id == current_user.id.to_s
      current_user.to_json
    end

    patch "/users/:user_id", provides: :json do |user_id|
      halt 404 unless user_id == current_user.id.to_s
      invalid_request "Cannot change acceptance of terms and conditions" if params["accepted_terms_and_conditions"]

      updateable = ["username", "first_name", "last_name", "allow_marketing_communications"]
      updates = params.select { |k, v| updateable.include?(k) }
      invalid_request "No updateable attributes specified" if updates.empty?

      begin
        current_user.update_attributes!(updates)
      rescue => e
        invalid_request e.message
      end

      current_user.to_json
    end

    get "/session" do
      refresh_token = validate_refresh_token
      handle_token_info_request(refresh_token)
    end

    post "/session" do
      refresh_token = validate_refresh_token
      handle_extend_token_info_request(refresh_token)
    end

    post "/password/change" do
      new_password = @params[:new_password]
      old_password = @params[:old_password]
      invalid_request "new_password_missing", "The new password is not provided." if new_password.nil? || new_password.empty?

      user = User.authenticate(current_user.username, old_password, request.ip)
      invalid_request "old_password_invalid", "Current password provided is incorrect." if user.nil?

      current_user.password = new_password
      current_user.save! rescue invalid_request("new_password_too_short", "The new password is too short.")

      Blinkbox::Zuul::Server::Email.password_confirmed(current_user)
      nil # no entity-body needed
    end

    post "/password/reset" do
      username = params[:username]
      invalid_request "The username is required." if username.nil? || username.empty?

      user = User.find_by_username(username)
      if user
        reset_token = PasswordResetToken.new do |t|
          t.user = user
          t.token = generate_opaque_token
        end
        reset_token.save!

        reset_url = settings.properties[:password_reset_url] % { token: reset_token.token }
        Blinkbox::Zuul::Server::Email.password_reset(user, reset_url, reset_token.token)
      end

      nil # no entity-body needed
    end

    post "/password/reset/validate-token" do
      token_value = params["password_reset_token"]
      invalid_request "A password reset token is required" if token_value.nil? || token_value.empty?

      password_reset_token = PasswordResetToken.find_by_token(token_value)
      invalid_request "The password reset token is invalid" if password_reset_token.nil?
      invalid_request "The password reset token has expired" if password_reset_token.expired?
      invalid_request "The password reset token has been revoked" if password_reset_token.revoked?
      nil # no entity-body needed
    end

    private

    def register_client
      create_client(current_user)
    end

    def create_client(user)
      missing_info = %w{client_name client_brand client_model client_os}.select { |key| params[key].nil? }
      invalid_request "invalid_client_info", "#{missing_info.first} must be supplied" if missing_info.any?

      client = Client.new do |c|
        c.name = params["client_name"]
        c.brand = params["client_brand"]
        c.model = params["client_model"]
        c.os = params["client_os"]
        c.user = user
        c.client_secret = generate_opaque_token
      end

      begin
        client.save!
      rescue => e
        if e.message == "Validation failed: #{UserClientsValidator.max_clients_error_message}"
          invalid_request "client_limit_reached", e.message
        else
          invalid_request e.message
        end
      end

      client
    end

    def handle_token_request(params)
      case params["grant_type"]
      when "password"
        handle_password_flow(params)
      when "refresh_token"
        handle_refresh_token_flow(params)
      when "urn:blinkbox:oauth:grant-type:password-reset-token"
        handle_password_reset_flow(params)
      when "urn:blinkbox:oauth:grant-type:registration"
        handle_registration_flow(params)
      else
        invalid_request "The grant type '#{params["grant_type"]}' is not supported"
      end
    end

    def handle_registration_flow(params)
      client_ip = IPAddress.parse(request.ip)
      unless client_ip.loopback? || client_ip.private?
        detected_country = @@geoip.country(request.ip)
        if detected_country.nil? || !["GB", "IE"].include?(detected_country.country_code2)
          invalid_request "country_geoblocked", "You must be in the UK to register"
        end
      end

      invalid_request "You must accept the terms and conditions" unless params["accepted_terms_and_conditions"] == "true"

      user = User.new do |u|
        u.first_name = params["first_name"]
        u.last_name = params["last_name"]
        u.username = params["username"]
        u.password = params["password"]
        u.allow_marketing_communications = params["allow_marketing_communications"]
      end

      client = nil
      error = nil

      ActiveRecord::Base.transaction do
        begin
          client = create_client(user) if %w{client_name client_brand client_model client_os}.select{ |key| !params[key].nil? }.any?
          user.save!
        rescue ActiveRecord::RecordInvalid => e
          error = {}
          if user.errors[:username].include?(user.errors.generate_message(:username, :taken))
            error[:reason] = "username_already_taken"
            error[:description] = e.message
          elsif e.message == "Validation failed: #{UserClientsValidator.max_clients_error_message}"
            error[:reason] = "client_limit_reached"
            error[:description] = e.message
          else
            error[:description] =  e.message
          end
        end
        raise ActiveRecord::Rollback if error
      end

      error[:reason].nil? ? invalid_request(error[:description]) : invalid_request(error[:reason], error[:description]) if error

      Blinkbox::Zuul::Server::Email.welcome(user)

      issue_refresh_token(user, client, true)
    end

    def handle_password_flow(params)
      username, password = params["username"], params["password"]
      invalid_request "The username and password are required for this grant type" if username.nil? || password.nil?

      user = User.authenticate(username, password, request.ip) 
      invalid_grant "The username and/or password is incorrect." if user.nil?

      client = authenticate_client(params, user)
      issue_refresh_token(user, client)
    end

    def handle_refresh_token_flow(params)
      token_value = params["refresh_token"]
      invalid_request "The refresh token is required for this grant type" if token_value.nil?

      refresh_token = RefreshToken.find_by_token(token_value)
      invalid_grant "The refresh token is invalid" if refresh_token.nil?
      invalid_grant "The refresh token has expired" if refresh_token.expires_at.past?
      invalid_grant "The refresh token has been revoked" if refresh_token.revoked

      client = authenticate_client(params, refresh_token.user)
      if refresh_token.client.nil?
        refresh_token.client = client
      elsif refresh_token.client != client
        invalid_client "Your client is not authorised to use this refresh token."
      end

      refresh_token.extend_lifetime
      refresh_token.save!

      issue_access_token(refresh_token, true)
    end

    def handle_password_reset_flow(params)
      token_value, new_password = params["password_reset_token"], params["password"]
      invalid_request "A password reset token is required for this grant type" if token_value.nil? || token_value.empty?
      invalid_request "A new password is required for this grant type" if new_password.nil? || new_password.empty?

      password_reset_token = PasswordResetToken.find_by_token(token_value)
      invalid_grant "The password reset token is invalid" if password_reset_token.nil?
      invalid_grant "The password reset token has expired" if password_reset_token.expired?
      invalid_grant "The password reset token has been revoked" if password_reset_token.revoked?

      user = password_reset_token.user
      client = authenticate_client(params, user)

      user.password = new_password
      user.password_reset_tokens.each { |token| token.revoked = true }
      ActiveRecord::Base.transaction do
        begin
          user.save!
        rescue ActiveRecord::RecordInvalid => e
          invalid_request e.message
        end
        user.password_reset_tokens.each { |token| token.save! if token.changed? }
      end

      issue_refresh_token(user, client)
    end

    def validate_refresh_token
      refresh_token = RefreshToken.find(env["zuul.claims"]["zl/rti"]) rescue nil

      www_authenticate_error("invalid_token", reason: "unverified_identity", description: "Access token is invalid") if refresh_token.nil?

      #invalid_grant "The refresh token is invalid" if refresh_token.nil?
      description = "It has been too long since you last verified your credentials."
      invalid_token = (refresh_token.status == RefreshToken::Status::INVALID) || refresh_token.expires_at.past?
      www_authenticate_error("invalid_token", reason: "unverified_identity", description: description) if invalid_token

      refresh_token
    end

    def handle_token_info_request(refresh_token)
      token_info = {}

      if refresh_token.elevation_expires_at.future?
        expiry_time = case refresh_token.elevation
                      when RefreshToken::Elevation::CRITICAL
                        refresh_token.critical_elevation_expires_at
                      else
                        refresh_token.elevation_expires_at
                      end

        token_info = {
          "token_status" => refresh_token.status,
          "token_elevation" => refresh_token.elevation,
          "token_elevation_expires_in" => expiry_time.to_i - DateTime.now.to_i
        }
      else
        if refresh_token.status == RefreshToken::Status::INVALID
          token_info = { "token_status" => RefreshToken::Status::INVALID }
        else
          token_info = {
            "token_elevation" => refresh_token.elevation,
            "token_status" => refresh_token.status
          }
        end
      end
      
      token_info["user_roles"] = refresh_token.user.role_names if refresh_token.user.roles.any?
      
      json token_info
    end

    def handle_extend_token_info_request(refresh_token)
      www_authenticate_error("invalid_token", reason: "unverified_identity", description: "User identity must be reverified") unless refresh_token.elevated?
      refresh_token.extend_elevation_time
      handle_token_info_request(refresh_token)
    end

    def authenticate_client(params, user)
      client_id, client_secret = params["client_id"], params["client_secret"]
      invalid_client "Both client id and client secret are required." if client_id.nil? ^ client_secret.nil?

      unless client_id.nil?
        client = Client.authenticate(client_id, client_secret)
        invalid_client "The client id and/or client secret is incorrect." if client.nil?
        invalid_client "You are not authorised to use this client." unless client.user == user
      end

      client.touch unless client.nil?
      client
    end

    def issue_refresh_token(user, client = nil, include_client_secret=false)
      refresh_token = RefreshToken.new do |rt|
        rt.user = user
        rt.client = client
        rt.token = generate_opaque_token
      end
      refresh_token.save!

      issue_access_token(refresh_token, true, include_client_secret)
    end

    def issue_access_token(refresh_token, include_refresh_token = false, include_client_secret = false)
      expires_in = settings.properties[:access_token_duration].to_i
      token_info = {
        "access_token" => build_access_token(refresh_token, expires_in),
        "token_type" => "bearer",
        "expires_in" => expires_in,
      }
      token_info["refresh_token"] = refresh_token.token if include_refresh_token
      token_info.merge!(refresh_token.user.as_json(format: :basic))
      token_info.merge!(build_client_info(refresh_token.client, include_client_secret)) unless refresh_token.client.nil?
      json token_info
    end

    def build_client_info(client, include_client_secret = false)
      client_info = {
        "client_id" => "urn:blinkbox:zuul:client:#{client.id}",
        "client_uri" => "/clients/#{client.id}",
        "client_name" => client.name,
        "client_brand" => client.brand,
        "client_model" => client.model,
        "client_os" => client.os,
        "last_used_date" => client.updated_at.utc.strftime("%F")
      }
      client_info["client_secret"] = client.client_secret if include_client_secret
      client_info
    end

    def build_access_token(refresh_token, expires_in)
      expires_at = DateTime.now + (expires_in / 86400.0)
      claims = {
        "sub" => "urn:blinkbox:zuul:user:#{refresh_token.user.id}",
        "exp" => expires_at.to_i
      }
      claims["bb/cid"] = "urn:blinkbox:zuul:client:#{refresh_token.client.id}" if refresh_token.client
      claims["bb/rol"] = refresh_token.user.role_names if refresh_token.user.roles.any?
      claims["zl/rti"] = refresh_token.id # for checking whether the issuing token has been revoked

      sig_key_id = settings.properties[:signing_key_id]
      signer = Sandal::Sig::ES256.new(File.read("./keys/#{sig_key_id}/private.pem"))
      jws_token = Sandal.encode_token(claims, signer, { "kid" => sig_key_id })

      enc_key_id = settings.properties[:encryption_key_id]
      encrypter = Sandal::Enc::A128GCM.new(Sandal::Enc::Alg::RSA_OAEP.new(File.read("./keys/#{enc_key_id}/public.pem")))
      Sandal.encrypt_token(jws_token, encrypter, { "kid" => enc_key_id, "cty" => "JWT" })
    end

    def generate_opaque_token
      Sandal::Util.jwt_base64_encode(SecureRandom.random_bytes(32))
    end

  end
end
