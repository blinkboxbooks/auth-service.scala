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

    def require_user_authorization_for(url_pattern)
      before url_pattern do
        access_token = request.bearer_token
        unless access_token.nil?
          begin
            claims = Sandal.decode_token(access_token) do |header|
              key_id = header["kid"]
              if key_id =~ %r{/sig/}
                Sandal::Sig::ES256.new(File.read("./keys/#{key_id}/public.pem"))
              elsif key_id =~ %r{/enc/}
                Sandal::Enc::A128GCM.new(Sandal::Enc::Alg::RSA_OAEP.new(File.read("./keys/#{key_id}/private.pem")))
              else
                throw Sandal::TokenError.new("Key #{header["kid"]} is unknown.")
              end
            end
            user_id = claims["sub"].match(/\Aurn:blinkbox:zuul:user:(\d+)\Z/)[1] if claims.has_key?("sub")
            request[:current_user] = Blinkbox::Zuul::Server::User.find_by_id(user_id)
          rescue Sandal::TokenError
            # TODO: Log it or anything?
          end
        end
        if request[:current_user].nil?
          halt 401, "User authorisation is required"
        end
      end
    end

  end
end