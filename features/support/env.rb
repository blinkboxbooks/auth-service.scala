require "httparty"
require "net/http/capture" # BUGBUG: httparty/capture should work; bug in HttpCapture I think...
require "cucumber/rest/steps/caching"
require "cucumber/rest/status"
require "rack/test"
require "timecop"
require "thin"


SERVER_URI = URI.parse(ENV["AUTH_SERVER"] || "http://localhost:9393/")
PROXY_URI = ENV["PROXY_SERVER"] ? URI.parse(ENV["PROXY_SERVER"]) : nil
IN_PROC= ENV["IN_PROC"]

Before do
  $zuul = ZuulClient.new(SERVER_URI, PROXY_URI)
  $server_ready = false
  if IN_PROC && !$server_up
    require_relative "../../lib/blinkbox/zuul/server"
    Thread.new {
      $server = Thin::Server.new('0.0.0.0', 9393, Blinkbox::Zuul::Server::App)
      $server.start
    }

    $server_up = true
  end

  loop until !$server.nil? && $server.running? if IN_PROC
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
World(SleepsByTimeTravel) if IN_PROC

