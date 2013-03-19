require 'sinatra/base'
require 'openssl'
require 'json'
require './../lib/oauth2_server/accesstoken'

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

    access_token = OAuth2Server::AccessToken.new
    access_token.issued = Time.now
    access_token.expires = access_token.issued + 2 # TODO: Units?
    access_token.userId = 516

    JSON.generate({
      access_token: OAuth2Server::SymmetricToken.encrypt_access_token(access_token),
      expires_in: access_token.expires - access_token.issued # TODO: Units?
      # TODO: Other fields?
    })
  end

end