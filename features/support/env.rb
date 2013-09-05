require "httparty"
require "net/http/capture" # BUGBUG: httparty/capture should work; bug in HttpCapture I think...
require "cucumber/rest/steps/caching"
require "cucumber/rest/status"

SERVER_URI = URI.parse(ENV["AUTH_SERVER"] || "http://localhost:9393/")
PROXY_URI = ENV["PROXY_SERVER"] ? URI.parse(ENV["PROXY_SERVER"]) : nil
TIME_MEASUREMENT= ENV["TIME_MEASUREMENT"] || "minutes"

Before do
  $zuul = ZuulClient.new(SERVER_URI, PROXY_URI)
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
