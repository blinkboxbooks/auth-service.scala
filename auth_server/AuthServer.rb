require 'sinatra/base'
require 'openssl'
require 'json'
require 'sandal'

class AuthServer < Sinatra::Base

  get '/oauth2/token' do
    grant_type = params[:grant_type]
    case grant_type
    when 'password'
      handle_password_flow(params)
    else
      halt 400
    end
  end

  private

  def handle_password_flow(params)
    username = params[:username]
    password = params[:password]
    halt 401 unless username == 'greg@blinkbox.com' && password == '1234$abcd'

    expires_in = 3600
    JSON.generate({
      access_token: create_access_token(expires_in),
      expires_in: expires_in
    })
  end

  def create_access_token(expires_in)
    issued_at = Time.now
    claims = JSON.generate({
      sub: 'urn:blinkboxbooks:id:user:18273',         # user id
      iat: issued_at.to_i,                            # issued at
      exp: (issued_at + expires_in).to_i,             # expires
      jti: '15b29d70-4e54-4286-be17-e9ba97d45194',    # token identifier
      bbb_dcc: 'GB',             # detected country code
      bbb_rcc: 'GB',             # registered country code
      bbb_rol: [1, 2, 8, 13],    # user roles
      bbb_did: 716352,           # device identifier
      bbb_dcl: 27                # device class
    })

    jws_key = OpenSSL::PKey::EC.new(File.read('./keys/auth_server_ec_priv.pem'))
    jws_signer = Sandal::Sig::ES256.new(jws_key)
    jws_token = Sandal.encode_token(claims, jws_signer, { kid: '/bbb/auth/ec/1' })

    # TODO: Uncomment to use an RSA signed token instead of an ECDSA one
    # jws_key = OpenSSL::PKey::RSA.new(File.read('./keys/auth_server_rsa_priv.pem'))
    # jws_signer = Sandal::Sig::RS256.new(jws_key)
    # jws_token = Sandal.encode_token(claims, jws_signer, { kid: '/bbb/auth/rsa/1' })

    jwe_key = OpenSSL::PKey::RSA.new(File.read('./keys/resource_server_rsa_pub.pem'))
    jwe_encrypter = Sandal::Enc::AES128CBC.new(jwe_key)
    Sandal.encrypt_token(jws_token, jwe_encrypter, { cty: 'JWT', kid: '/bbb/svcs/rsa/1' })
  end

end