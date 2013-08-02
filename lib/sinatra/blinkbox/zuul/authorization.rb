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
            user_id = request.env["zuul.user_id"]
            request.env["zuul.user"] = ::Blinkbox::Zuul::Server::User.find_by_id(user_id) if user_id
            halt 401 if request.env["zuul.user"].nil?
          end
        end

      end
    end
  end
end