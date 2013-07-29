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