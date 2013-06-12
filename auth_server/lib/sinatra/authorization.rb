module Sinatra
  module Authorization

    module Helpers
      def current_user
        request[:current_user]
      end
    end

    def self.registered(app)
      app.helpers Sinatra::Authorization::Helpers
    end

    def require_user_authorization_for(url_pattern, message = "User authorisation is required")
      before url_pattern do
        auth_header = request.env["HTTP_AUTHORIZATION"]
        unless auth_header.nil?
          auth_scheme, access_token = auth_header.split(" ", 2)
          if auth_scheme == "Bearer"
            claims = Sandal.decode_token(access_token) do |header|
              case header["kid"]
              when "/bbb/auth/sig/ec/1"
                Sandal::Sig::ES256.new(File.read("./keys/auth_server_ec_priv.pem"))
              when "/bbb/auth/sig/rsa/1"
                Sandal::Sig::RS256.new(File.read("./keys/auth_server_rsa_priv.pem"))
              when "/bbb/svcs/enc/a128/1"
                Sandal::Enc::A128GCM.new(Sandal::Enc::Alg::Direct.new(File.read("./keys/shared_aes128.key")))
              else
                throw Sandal::TokenError.new("Key #{header["kid"]} is unknown.")
              end
            end rescue nil
          end
        end

        request[:current_user] = User.find_by_id(claims["bbb/uid"]) unless claims.nil?
        if request[:current_user].nil?
          halt 401, message
        end
      end
    end

  end
end