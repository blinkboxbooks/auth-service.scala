module Sinatra
  module OAuthHelper

    def base_url
      @base_url ||= "#{request.env["rack.url_scheme"]}://#{request.env["HTTP_HOST"]}"
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

    def method_missing(method_sym, *args)
      if method_sym =~ /invalid_[a-z]+/
        oauth_error(method_sym.to_s, *args)
      else
        super
      end
    end

  end
end