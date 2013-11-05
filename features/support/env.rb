require "httparty"
require "net/http/capture" # BUGBUG: httparty/capture should work; bug in HttpCapture I think...
require "cucumber/rest/steps/caching"
require "cucumber/rest/status"
require "ipaddress"
require "ipaddress/ipv4_loopback"
require "rack/test"
require "timecop"
require "thin"
require "blinkbox/zuul/server/email"

TEST_CONFIG = {}
TEST_CONFIG[:server] = URI.parse(ENV["AUTH_SERVER"] || "http://127.0.0.1:9393/")
TEST_CONFIG[:proxy] = ENV["PROXY_SERVER"] ? URI.parse(ENV["PROXY_SERVER"]) : nil
TEST_CONFIG[:in_proc] = if ENV["IN_PROC"] =~ /^(false|no)$/i
                          false
                        else
                          host = TEST_CONFIG[:server].host
                          IPAddress.valid?(host) && IPAddress.parse(host).loopback?
                        end

if TEST_CONFIG[:in_proc]
  require_relative "../../lib/blinkbox/zuul/server" 
  $server = Thin::Server.new(TEST_CONFIG[:server].host, TEST_CONFIG[:server].port, Blinkbox::Zuul::Server::App)
  Thread.new { $server.start }
  loop until $server.running?
end

# TODO: parametrise these
ELEVATION_CONFIG = {
  critical_timespan: (Blinkbox::Zuul::Server::RefreshToken::LifeSpan::CRITICAL_ELEVATION_LIFETIME_IN_SECONDS.to_i rescue 600),
  elevated_timespan: (Blinkbox::Zuul::Server::RefreshToken::LifeSpan::NORMAL_ELEVATION_LIFETIME_IN_SECONDS.to_i rescue 86400)
}

Before do
  $zuul = ZuulClient.new(TEST_CONFIG[:server], TEST_CONFIG[:proxy])
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

module SendsMessagesToFakeQueues
  module FakeEmail
    def self.included(base)
      base.instance_eval do
        @sent_messages = []
        def sent_messages
          @sent_messages
        end
        def enqueue(message)
          @sent_messages.push(message)
        end
      end
    end
  end  
  def self.extended(base)
    Blinkbox::Zuul::Server::Email.send(:include, FakeEmail)
  end
end

World(KnowsAboutResponses)
World(SleepsByTimeTravel) if TEST_CONFIG[:in_proc]
World(SendsMessagesToFakeQueues) if TEST_CONFIG[:in_proc]