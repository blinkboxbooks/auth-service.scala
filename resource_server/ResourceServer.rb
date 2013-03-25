require 'sinatra/base'
require 'openssl'
require 'sandal'

class ResourceServer < Sinatra::Base

  get '/' do
  auth_header = request.env['HTTP_AUTHORIZATION']
  halt 401 if auth_header.nil?
  auth_scheme = auth_header[0..(auth_header.index(' ') - 1)]
  halt 401 unless auth_scheme == 'Bearer'
  access_token = auth_header[(auth_header.index(' ') + 1)..-1]

  jws_token = Sandal.decrypt_token(access_token) do |header|
    case header['kid']
    when '/bbb/svcs/enc/rsa/1'
      jwe_key = OpenSSL::PKey::RSA.new(File.read('./keys/resource_server_rsa_priv.pem'))
      Sandal::Enc::AES128CBC.new(jwe_key)
    else
      throw SecurityError.new('Signing key not known')
    end
  end

  claims = Sandal.decode_token(jws_token) do |header| 
    case header['kid']
    when '/bbb/auth/sig/ec/1'
      jws_key = OpenSSL::PKey::EC.new(File.read('./keys/auth_server_ec_pub.pem'))
      Sandal::Sig::ES256.new(jws_key)
    when '/bbb/auth/sig/rsa/1'
      jwe_key = OpenSSL::PKey::RSA.new(File.read('./keys/auth_server_rsa_pub.pem'))
      Sandal::Sig::RS256.new(jwe_key)
    else
      throw SecurityError.new('Signing key not known')
    end
  end

  # just reflect the claims we got back as the response
  claims.to_s
  end

end