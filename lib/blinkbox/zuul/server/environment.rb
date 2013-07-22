require "sinatra/base"
require "active_record"
require "java_properties"
require "uri"

module Blinkbox
  module Zuul
    module Server 
      class App < Sinatra::Base

        configure do  
          @properties = JavaProperties::Properties.new(".properties")

          db = URI.parse(@properties["database_url"])
          ActiveRecord::Base.establish_connection(
            adapter:  db.scheme == "postgres" ? "postgresql" : db.scheme,
            host:     db.host,
            username: db.user,
            password: db.password,
            database: db.path[1..-1],
            encoding: "utf8",
            pool:     20
          )

          Dir.glob(File.join(File.dirname(__FILE__), "models", "*.rb")).each { |file| require file }
        end

      end
    end
  end
end