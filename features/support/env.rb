require "httparty"
require "net/http/capture" # BUGBUG: httparty/capture should work; bug in HttpCapture I think...
require "cucumber/rest/steps/caching"

module KnowsAboutTheEnvironment
  def servers
    @servers ||= {
      auth: URI.parse(ENV["AUTH_SERVER"] || "http://localhost:9393/")
    }
  end
end
World(KnowsAboutTheEnvironment)

module KnowsAboutResponses
  def last_response
    HttpCapture::RESPONSES.last
  end
  def last_response_json
    MultiJson.load(HttpCapture::RESPONSES.last.body)
  end
end
World(KnowsAboutResponses)

Before do
  $zuul = ZuulClient.new(servers[:auth])
end