module Sinatra
  module Authorization

    module Helpers
      def current_user
        request.env[:current_user]
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
              key_dir = "./keys" + File.expand_path(header["kid"], "/")
              if key_dir =~ %r{/sig/}
                raise Sandal::TokenError.new("Unsupported signature algorithm") unless header["alg"] == Sandal::Sig::ES256::NAME
                Sandal::Sig::ES256.new(File.read("#{key_dir}/public.pem"))
              elsif key_dir =~ %r{/enc/}
                raise Sandal::TokenError.new("Unsupported encryption method") unless header["enc"] == Sandal::Enc::A128GCM::NAME
                raise Sandal::TokenError.new("Unsupported encryption algorithm") unless header["alg"] == Sandal::Enc::Alg::RSA_OAEP::NAME
                Sandal::Enc::A128GCM.new(Sandal::Enc::Alg::RSA_OAEP.new(File.read("#{key_dir}/private.pem")))
              else
                raise Sandal::TokenError.new("Key #{header["kid"]} is unknown.")
              end
            end
          rescue Sandal::TokenError => e
            # TODO: Better format for logging (discuss with ops)
            logger.warn(e.message)
          end

          if claims && claims.has_key?("sub")
            user_id = claims["sub"].match(/\Aurn:blinkbox:zuul:user:(\d+)\Z/)[1]
            request.env[:current_user] = Blinkbox::Zuul::Server::User.find_by_id(user_id)
          end
        end

        if request.env[:current_user].nil?
          halt 401, "User authorisation is required"
        end
      end
    end

  end
end