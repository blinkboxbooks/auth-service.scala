require "mechanize"

module KnowsAboutTheEnvironment
  def servers
    @servers ||= {
      auth: URI.parse(ENV["AUTH_SERVER"] || "http://localhost:9393/")
    }
  end
end

World(KnowsAboutTheEnvironment)

Before do 
  @agent = Mechanize.new
end