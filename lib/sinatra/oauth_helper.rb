require "sinatra/base"

module Sinatra
  module OAuthHelper

    def base_url
      # Pound load balancer sends the forwarded protocol in the HTTP_X_FORWARDED_PROTO header
      # we make the assumption that it is ensuring the user isn't overwriting this.
      scheme = request.env['HTTP_X_FORWARDED_PROTO'] || request.env["rack.url_scheme"]
      @base_url ||= "#{scheme}://#{request.env["HTTP_HOST"]}"
    end

    def oauth_error(code, *args)
      case args.length
      when 0
        halt 400, json({ "error" => code })
      when 1
        halt 400, json({ "error" => code, "error_description" => args[0] })
      else
        halt 400, json({ "error" => code, "error_reason" => args[0], "error_description" => args[1] })
      end
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