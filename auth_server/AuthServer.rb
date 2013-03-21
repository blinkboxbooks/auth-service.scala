require 'rubygems'
require 'bundler'

Bundler.require

require 'sinatra/base'
require 'openssl'
require 'json'
require '../lib/jwt'

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
      iss: 'blinkboxbooks',                           # issuer
      aud: 'blinkboxbooks',                           # audience
      sub: 'urn:blinkboxbooks:id:user:18273',         # user id
      iat: issued_at.to_i,                            # issued at
      exp: (issued_at + expires_in).to_i,                   # expires
      jti: '15b29d70-4e54-4286-be17-e9ba97d45194',    # token identifier
      bbb_dcc: 'GB',             # detected country code
      bbb_rcc: 'GB',             # registered country code
      bbb_rol: [1, 2, 8, 13],    # user roles
      bbb_did: 716352,           # device identifier
      bbb_dcl: 27                # device class
    })
    encoded_claims = JWT.base64_encode(claims)

    jws_key = OpenSSL::PKey::RSA.new(File.read('./keys/auth_server_priv.pem'))
    jws_token = JWT.signed_token(encoded_claims, jws_key, { kid: 'bbb-as-1' })
    jwe_key = OpenSSL::PKey::RSA.new(File.read('./keys/resource_server_pub.pem'))
    JWT.encrypted_token(jws_token, jwe_key, { cty: 'JWT', kid: 'bbb-rs-1' })
  end

end