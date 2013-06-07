require_relative "common_transforms"

module KnowsAboutServers
  def servers
    @servers ||= {}
  end
end
World(KnowsAboutServers)

Given(/^that the (.+) server is at "(#{CAPTURE_URI})"$/) do |server, uri|
  servers[server] = uri
end