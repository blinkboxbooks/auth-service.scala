require 'sinatra/base'
require 'openssl'
require './../lib/oauth2_server/accesstoken'

class ResourceServer < Sinatra::Base

  get '/' do

    auth_header = request.env['HTTP_AUTHORIZATION']
    halt 401 if auth_header.nil?
    auth_scheme = auth_header[0..(auth_header.index(' ') - 1)]
    halt 401 unless auth_scheme == 'Bearer'
    access_token_string = auth_header[(auth_header.index(' ') + 1)..-1]

    access_token = OAuth2Server::SymmetricToken.decrypt_access_token(access_token_string)

    access_token.to_s

  end

end