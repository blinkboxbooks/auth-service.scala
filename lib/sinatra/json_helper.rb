require "sinatra/base"
require "json"

module Sinatra
  module JSONHelper
    def json(data)
      content_type :json
      data.is_a?(String) ? data : ::JSON.generate(data)
    end
  end

  helpers JSONHelper
end