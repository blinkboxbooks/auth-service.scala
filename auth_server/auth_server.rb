require "sinatra/base"
require "openssl"
require "multi_json"
require "sandal"
require "scrypt"
require "./environment"
require "./lib/auth_server/model"

class AuthServer < Sinatra::Base
  include Sandal::Util

  ACCESS_TOKEN_LIFETIME = 1800
  REFRESH_TOKEN_LIFETIME = 3600 * 24 * 90
  MIN_PASSWORD_LENGTH = 6

  helpers do
    def oauth_error(code, description)
      content_type :json
      halt 400, MultiJson.dump({ "error" => code, "error_description" => description })
    end

    def invalid_grant(description)
      oauth_error "invalid_grant", description
    end

    def invalid_request(description)
      oauth_error "invalid_request", description
    end
  end

  get "/oauth2/token" do # discouraged, but required by the spec
    handle_token_request(params)
  end

  post "/oauth2/token" do
    handle_token_request(params)
  end

  private

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
    
    password_hash = SCrypt::Password.create(password)
    user = User.new(first_name: first_name, last_name: last_name, email: email, password_hash: password_hash) 
    begin
      user.save!
    rescue ActiveRecord::RecordInvalid => e
      invalid_request e.message
    rescue ActiveRecord::RecordNotUnique => e
      invalid_request "An account with that email address already exists"
    end
    
    issue_new_tokens(user)
  end

  def handle_password_flow(params)
    email, password = params[:username], params[:password]
    if email.nil? || password.nil?
      invalid_request "The 'username' and 'password' parameters are required for the 'password' grant type"
    end

    user = User.find_by_email(email)
    unless user && SCrypt::Password.new(user.password_hash) == password
      invalid_grant "The email and/or password is incorrect."
    end

    issue_new_tokens(user)
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

    if refresh_token.updated_at > (Time.now - (3600 * 24))
      issue_new_access_token(refresh_token)
    else
      issue_new_tokens(refresh_token.user)
    end
  end

  def issue_new_tokens(user, device = nil)
    refresh_token = RefreshToken.new do |token|
      token.user = user
      token.device = device
      token.token = jwt_base64_encode(SecureRandom.random_bytes(32))
      token.expires_at = Time.now + REFRESH_TOKEN_LIFETIME
    end
    issue_new_access_token(refresh_token)
  end

  def issue_new_access_token(refresh_token)
    refresh_token.access_token = AccessToken.new do |token|
      token.expires_at = Time.now + ACCESS_TOKEN_LIFETIME
    end
    refresh_token.save!
    format_token_response(refresh_token)
  end

  def format_token_response(refresh_token)
    MultiJson.dump({
      "access_token" => format_access_token(refresh_token),
      "token_type" => "bearer",
      "expires_in" => ACCESS_TOKEN_LIFETIME,
      "refresh_token" => refresh_token.token
    })
  end

  def format_access_token(refresh_token)
    issued_at = Time.now
    claims = MultiJson.dump({
      "sub" => "urn:blinkboxbooks:id:user:#{refresh_token.user.id}",
      "iat" => issued_at.to_i,
      "exp" => refresh_token.access_token.expires_at.to_i,
      "jti" => refresh_token.access_token.id.to_s,
      "bbb/uid" => refresh_token.user.id,
    })
    if refresh_token.device
      claims["bbb/cid"] = "urn:blinkboxbooks:id:device:#{refresh_token.device.id}"
    end

    signer = Sandal::Sig::ES256.new(File.read("./keys/auth_server_ec_priv.pem"))
    jws_token = Sandal.encode_token(claims, signer, { "kid" => "/bbb/auth/sig/ec/1" })

    # TODO: Uncomment to use an RSA signed token instead of an ECDSA one
    # signer = Sandal::Sig::RS256.new(File.read("./keys/auth_server_rsa_priv.pem"))
    # jws_token = Sandal.encode_token(claims, signer, { kid: "/bbb/auth/sig/rsa/1" })

    encrypter = Sandal::Enc::A128GCM.new(Sandal::Enc::Alg::Direct.new(File.read("./keys/shared_aes128.key")))
    Sandal.encrypt_token(jws_token, encrypter, { "kid" => "/bbb/svcs/enc/a128/1", "cty" => "JWT" })
  end

end