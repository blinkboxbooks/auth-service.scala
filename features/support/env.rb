require "httparty"
require "net/http/capture" # BUGBUG: httparty/capture should work; bug in HttpCapture I think...
require "cucumber/rest/steps/caching"
require "cucumber/rest/status"

Before do
  $zuul = ZuulClient.new(ENV["AUTH_SERVER"] || "http://localhost:9393/")
end

module KnowsAboutResponses
  def last_response
    HttpCapture::RESPONSES.last
  end
  def last_response_json
    MultiJson.load(last_response.body)
  end
end
World(KnowsAboutResponses)