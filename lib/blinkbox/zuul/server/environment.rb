require "sinatra/base"
require "active_record"
require "java_properties"
require "uri"

module Blinkbox
  module Zuul
    module Server 
      class App < Sinatra::Base

        configure do
          propfile = ["app.properties", "app.properties.#{ENV["RACK_ENV"]}"].select { |f| File.exist?(f) }.first
          raise "No properties file found." unless propfile
          set :properties, JavaProperties::Properties.new(propfile)

          db = URI.parse(settings.properties["database_url"])
          ActiveRecord::Base.establish_connection(
            adapter:  case db.scheme
                      when "mysql" then "mysql2"
                      when "postgres" then "postgresql"
                      else db.scheme
                      end,
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