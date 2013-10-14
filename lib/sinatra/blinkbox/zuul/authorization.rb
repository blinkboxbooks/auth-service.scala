require "sinatra/base"
require "sandal"

module Sinatra
  module Blinkbox
    module Zuul
      module Authorization

        module Helpers
          def current_user
            request.env["zuul.user"]
          end
        end

        def self.registered(app)
          app.helpers Sinatra::Blinkbox::Zuul::Authorization::Helpers
        end

        def require_user_authorization_for(url_pattern)
          before url_pattern do
            if request.env["zuul.access_token"].nil?
              headers["WWW-Authenticate"] = "Bearer"
              halt 401
            end

            if request.env["zuul.access_token"].empty? || request.env["zuul.error"]
              headers["WWW-Authenticate"] = 'Bearer error="invalid_token", error_description="Access token is invalid"'
              halt 401
            end

            user_id = request.env["zuul.user_id"]
            request.env["zuul.user"] = ::Blinkbox::Zuul::Server::User.find_by_id(user_id) if user_id
            if request.env["zuul.user"].nil?
              headers["WWW-Authenticate"] = 'Bearer error="invalid_token", error_description="User not found"'
              halt 401
            end
          end
        end

      end
    end
  end
end