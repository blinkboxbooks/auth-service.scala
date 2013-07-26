require "sinatra/base"

module Sinatra
  class Request

    def bearer_token
      auth_header = self.env["HTTP_AUTHORIZATION"]
      return nil if auth_header.nil?
          
      auth_scheme, bearer_token = auth_header.split(" ", 2)
      return nil unless auth_scheme == "Bearer"

      bearer_token
    end

  end
end