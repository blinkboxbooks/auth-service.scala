require "sinatra/base"

module Sinatra
  module Blinkbox
    module Zuul
      module Elevation
        def self.registered(app)
          app.set(:methods) do |*methods|
            condition do
              break(true) if methods.include? :all
              methods.include? request.request_method.downcase.to_sym
            end
          end
        end

        def require_elevation_for(url_pattern, level: :critical, methods: :all)
          # @refresh_token is thread-safe cause App is created per request
          before url_pattern, methods: methods do
            @refresh_token = validate_refresh_token
            required_elevation = level == :elevated ? @refresh_token.elevated? : @refresh_token.critically_elevated?
            www_authenticate_error("invalid_token", reason: "unverified_identity", description: "User identity must be reverified") unless required_elevation
          end
          after url_pattern, methods: methods do
            @refresh_token.extend_elevation_time if !@refresh_token.nil? && response.successful?
          end
        end

      end
    end
  end
end