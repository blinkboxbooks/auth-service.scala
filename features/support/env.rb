require "httparty"
require "net/http/capture" # BUGBUG: httparty/capture should work; bug in HttpCapture I think...
require "cucumber/rest/steps/caching"
require "cucumber/rest/status"
require "rack/test"
require "timecop"
require_relative "../../lib/blinkbox/zuul/server"

SERVER_URI = URI.parse(ENV["AUTH_SERVER"] || "http://localhost:9393/")
PROXY_URI = ENV["PROXY_SERVER"] ? URI.parse(ENV["PROXY_SERVER"]) : nil
TIME_MEASUREMENT= ENV["TIME_MEASUREMENT"]

Before do
  $zuul = ZuulClient.new(SERVER_URI, PROXY_URI)
  if TIME_MEASUREMENT and not $server_up
    Thread.new {
      Rack::Handler::Thin.run(Blinkbox::Zuul::Server::App, :Port => 9393)
    }
    Kernel.sleep(5)
    $server_up = true
  end

end

module KnowsAboutResponses

  def last_response
    HttpCapture::RESPONSES.last
  end

  def last_response_json
    MultiJson.load(last_response.body)
  end
end

module SleepsByTimeTravel
  def sleep(duration)
    Timecop.travel(duration)
  end
end

World(KnowsAboutResponses)
World(SleepsByTimeTravel)

