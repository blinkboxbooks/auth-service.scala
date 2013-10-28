require "sinatra/base"

module Sinatra
  module WWWAuthenticateHelper

    def www_authenticate_error(error = nil, reason: nil, description: nil)
      details = []
      details << "error=\"#{error}\"" if error
      details << "error_reason=\"#{reason}\"" if reason
      details << "error_description=\"#{description}\"" if description

      headers['WWW-Authenticate'] = ["Bearer", details.join(", ")].join(" ")
      halt 401 
    end

  end

  helpers WWWAuthenticateHelper
end