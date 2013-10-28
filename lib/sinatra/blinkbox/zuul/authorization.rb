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
            www_authenticate_error if request.env["zuul.access_token"].nil?
            www_authenticate_error("invalid_token", description: "A token is required") if request.env["zuul.access_token"].empty?
            www_authenticate_error("invalid_token", description: "The access token expired") if request.env["zuul.error"].is_a? Sandal::ExpiredTokenError
            www_authenticate_error("invalid_token", description: "The access token is invalid") if request.env["zuul.error"]

            user_id = request.env["zuul.user_id"]
            request.env["zuul.user"] = ::Blinkbox::Zuul::Server::User.find_by_id(user_id) if user_id
            www_authenticate_error("invalid_token", description: "User not found") if request.env["zuul.user"].nil?
          end
        end

      end
    end
  end
end