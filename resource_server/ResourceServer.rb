require "sinatra/base"
require "openssl"
require "sandal"

class ResourceServer < Sinatra::Base

  before do
    @user = {}
    auth_header = request.env["HTTP_AUTHORIZATION"]
    return if auth_header.nil?
    auth_scheme, access_token = auth_header.split(" ", 2)
    return unless auth_scheme == "Bearer"
    @user[:access_token] = access_token
    begin
      @user.merge!(Sandal.decode_token(access_token) do |header|
        case header["kid"]
        when "/bbb/auth/sig/ec/1"
          Sandal::Sig::ES256.new(File.read("./keys/auth_server_ec_pub.pem"))
        when "/bbb/auth/sig/rsa/1"
          Sandal::Sig::RS256.new(File.read("./keys/auth_server_rsa_pub.pem"))
        when "/bbb/svcs/enc/a128/1"
          Sandal::Enc::A128CBC_HS256.new(Sandal::Enc::Alg::Direct.new(File.read("./keys/shared_aes128.key")))
        else
          throw Sandal::TokenError.new("Key #{header["kid"]} is unknown.")
        end
      end)
    rescue Sandal::TokenError => e
      @user[:access_token_error] = e
    end
  end

  before "/" do
    if @user["cid"].nil?
      halt 401, "Client id is required"
    end
  end

  get "/" do
    @user.to_s
  end

end