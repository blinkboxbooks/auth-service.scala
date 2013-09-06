require "sinatra/base"

module Sinatra
  module OAuthHelper

    def base_url
      @base_url ||= "#{request.env["rack.url_scheme"]}://#{request.env["HTTP_HOST"]}"
    end

    def oauth_error(code, *args, status_code: 400)
      case args.length
      when 0
        headers['WWW-Authenticate'] = "Bearer error=\"#{code}\""
      when 1
        headers['WWW-Authenticate'] = "Bearer error=\"#{code}\", error_description=\"#{args[0]}\""
      else
        headers['WWW-Authenticate'] = "Bearer error=\"#{code}\", error_reason=\"#{args[0]}\", error_description=\"#{args[1]}\""
      end
      halt status_code
    end

    def method_missing(method_sym, *args, status_code: 400)
      if method_sym =~ /invalid_[a-z]+/
        oauth_error(method_sym.to_s, *args, status_code: status_code)
      else
        super
      end
    end

  end

  helpers OAuthHelper
end