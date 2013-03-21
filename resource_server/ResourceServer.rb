require 'rubygems'
require 'bundler'

Bundler.require

require 'sinatra/base'
require 'openssl'
require '../lib/jwt'

class ResourceServer < Sinatra::Base

  get '/' do
    auth_header = request.env['HTTP_AUTHORIZATION']
    halt 401 if auth_header.nil?
    auth_scheme = auth_header[0..(auth_header.index(' ') - 1)]
    halt 401 unless auth_scheme == 'Bearer'
    access_token = auth_header[(auth_header.index(' ') + 1)..-1]

    decryption_key = OpenSSL::PKey::RSA.new(File.read('./keys/resource_server_priv.pem'))
    jws_token = JWT.decrypt_token(access_token, decryption_key)
    verification_key = OpenSSL::PKey::RSA.new(File.read('./keys/auth_server_pub.pem'))
    claims = JWT.decode_token(jws_token, verification_key)

    # just reflect the claims we got back as the response
    claims.to_s
  end

end