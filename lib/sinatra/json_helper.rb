module Sinatra
  module JSONHelper
    def json(data)
      content_type :json
      data.is_a?(String) ? data : MultiJson.dump(data)
    end
  end

  helpers JSON
end