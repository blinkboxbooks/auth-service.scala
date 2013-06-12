module Sinatra
  module OAuthHelper

    def base_url
      @base_url ||= "#{request.env["rack.url_scheme"]}://#{request.env["HTTP_HOST"]}"
    end

    def json(data)
      content_type :json
      data.is_a?(String) ? data : MultiJson.dump(data)
    end

    def oauth_error(code, description)
      content_type :json
      halt 400, MultiJson.dump({ "error" => code, "error_description" => description })
    end

    def invalid_grant(description)
      oauth_error "invalid_grant", description
    end

    def invalid_request(description)
      oauth_error "invalid_request", description
    end

  end
end