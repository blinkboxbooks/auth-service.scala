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

  claims = Sandal.decode_token(access_token) do |header|
    case header['kid']
    when '/bbb/auth/sig/ec/1'
      Sandal::Sig::ES256.new(File.read('./keys/auth_server_ec_pub.pem'))
    when '/bbb/auth/sig/rsa/1'
      Sandal::Sig::RS256.new(File.read('./keys/auth_server_rsa_pub.pem'))
    when '/bbb/svcs/enc/a128/1'
      alg = Sandal::Enc::Alg::Direct.new(File.read('./keys/shared_aes128.key'))
      Sandal::Enc::A128CBC_HS256.new(alg)
    else
      throw SecurityError.new('Key not known')
    end
  end

  # just reflect the claims we got back as the response
  claims.to_s
  end

end