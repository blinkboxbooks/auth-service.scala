require 'sinatra/base'
require 'openssl'
require 'json'
require 'sandal'

class AuthServer < Sinatra::Base

  get '/oauth2/token' do
    handle_token_request(params)
  end

  post '/oauth2/token' do
    handle_token_request(params)
  end

  private

  def handle_token_request(params)
    grant_type = params[:grant_type]
    if grant_type.nil?
      halt 400, JSON.generate({ 'error' => 'invalid_request', 'error_description' => 'The grant_type parameter is required.' })
    end

    case grant_type
    when 'password'
      handle_password_flow(params)
    when 'refresh_token'
      handle_refresh_token_flow(params)
    else
      halt 400, JSON.generate({ 'error' => 'unsupported_grant_type', 'error_description' => 'Use the "password" grant_type.' })
    end
  end

  def handle_password_flow(params)
    username = params[:username]
    password = params[:password]
    if username.nil? || password.nil?
      halt 400, JSON.generate({ 'error' => 'invalid_request', 'error_description' => 'The username and password parameters are required.' })
    end
    unless username == 'greg@blinkbox.com' && password == '1234$abcd'
      halt 400, JSON.generate({ 'error' => 'invalid_grant', 'error_description' => 'The username and/or password is incorrect.' })
    end

    expires_in = 1200
    JSON.generate({
      'access_token' => create_access_token(expires_in),
      'token_type' => 'bearer',
      'expires_in' => expires_in,
      'refresh_token' => Sandal::Util.base64_encode(SecureRandom.random_bytes(32))
    })
  end

  def handle_refresh_token_flow(params)
    refresh_token = params[:refresh_token]
    if refresh_token.nil?
      halt 400, JSON.generate({ 'error' => 'invalid_request', 'error_description' => 'The refresh_token parameter is required.' })
    end
    puts refresh_token.length
    unless refresh_token.length == 43 # just checking the length saves us actually storing it...
      halt 400, JSON.generate({ 'error' => 'invalid_grant', 'error_description' => 'The refresh_token is invalid.' })
    end

    expires_in = 1200
    JSON.generate({
      'access_token' => create_access_token(expires_in),
      'token_type' => 'bearer',
      'expires_in' => expires_in
    })
  end

  def create_access_token(expires_in)
    issued_at = Time.now
    claims = JSON.generate({
      'sub' => 'user@example.org',
      'iat' => issued_at.to_i,
      'exp' => (issued_at + expires_in).to_i,
    })

    jws_key = OpenSSL::PKey::EC.new(File.read('./keys/auth_server_ec_priv.pem'))
    jws_signer = Sandal::Sig::ES256.new(jws_key)
    jws_token = Sandal.encode_token(claims, jws_signer, { kid: '/bbb/auth/sig/ec/1' })

    # TODO: Uncomment to use an RSA signed token instead of an ECDSA one
    # jws_key = OpenSSL::PKey::RSA.new(File.read('./keys/auth_server_rsa_priv.pem'))
    # jws_signer = Sandal::Sig::RS256.new(jws_key)
    # jws_token = Sandal.encode_token(claims, jws_signer, { kid: '/bbb/auth/sig/rsa/1' })

    jwe_key = OpenSSL::PKey::RSA.new(File.read('./keys/resource_server_rsa_pub.pem'))
    jwe_encrypter = Sandal::Enc::AES128CBC.new(jwe_key)
    Sandal.encrypt_token(jws_token, jwe_encrypter, { cty: 'JWT', kid: '/bbb/svcs/enc/rsa/1' })
  end

end