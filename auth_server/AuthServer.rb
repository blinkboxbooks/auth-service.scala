require 'sinatra/base'
require 'openssl'
require 'json'
require 'sandal'

class AuthServer < Sinatra::Base
  include Sandal::Util

  ACCESS_TOKEN_LIFETIME = 1800

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

    JSON.generate({
      'access_token' => create_access_token,
      'token_type' => 'bearer',
      'expires_in' => ACCESS_TOKEN_LIFETIME,
      'refresh_token' => jwt_base64_encode(SecureRandom.random_bytes(32))
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

    JSON.generate({
      'access_token' => create_access_token,
      'token_type' => 'bearer',
      'expires_in' => ACCESS_TOKEN_LIFETIME
    })
  end

  def create_access_token()
    issued_at = Time.now
    claims = JSON.generate({
      'sub' => 'urn:blinkboxbooks:id:user:18374',
      'iat' => issued_at.to_i,
      'exp' => (issued_at + ACCESS_TOKEN_LIFETIME).to_i,
      'tid' => SecureRandom.uuid,
      'bbb/uid' => 18374,
      'bbb/cid' => 'urn:blinkboxbooks:id:device:294859'
    })

    signer = Sandal::Sig::ES256.new(File.read('./keys/auth_server_ec_priv.pem'))
    jws_token = Sandal.encode_token(claims, signer, { kid: '/bbb/auth/sig/ec/1' })

    # TODO: Uncomment to use an RSA signed token instead of an ECDSA one
    # signer = Sandal::Sig::RS256.new(File.read('./keys/auth_server_rsa_priv.pem'))
    # jws_token = Sandal.encode_token(claims, signer, { kid: '/bbb/auth/sig/rsa/1' })

    #key = OpenSSL::Cipher.new('aes-128-cbc').encrypt.random_key
    key = File.read('./keys/shared_aes128.key').encode('utf-8')
    alg = Sandal::Enc::Alg::Direct.new(key)
    encrypter = Sandal::Enc::A128CBC_HS256.new(alg)
    Sandal.encrypt_token(jws_token, encrypter, { cty: 'JWT', kid: '/bbb/svcs/enc/a128/1' })
  end

end