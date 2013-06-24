module Sinatra
  module Authorization

    module Helpers
      def current_client
        request[:current_client]
      end
      
      def current_user
        request[:current_user]
      end
    end

    def self.registered(app)
      app.helpers Sinatra::Authorization::Helpers
    end

    def require_client_authorization_for(url_pattern)
      before url_pattern do |client_id|
        registration_access_token = request.bearer_token
        unless registration_access_token.nil?
          request[:current_client] = Blinkbox::Zuul::Server::Client.find_by_registration_access_token(registration_access_token)
        end
        if request[:current_client].nil?
          halt 401, "Client authorisation is required"
        end
        unless request[:current_client].id == client_id.to_i
          halt 403, "Clients are only permitted to access their own information"
        end
      end
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
            user_id = claims["sub"].match(/^urn:blinkbox:zuul:user:(\d+)$/)[1] if claims.has_key?("sub")
            request[:current_user] = Blinkbox::Zuul::Server::User.find_by_id(user_id)
          rescue Sandal::TokenError
          end
        end
        if request[:current_user].nil?
          halt 401, "User authorisation is required"
        end
      end
    end

  end
end