require "sinatra/base"
require "active_record"
require "java_properties"
require "uri"
require "geoip"

module Blinkbox
  module Zuul
    module Server
      class App < Sinatra::Base

        PROPERTIES_FILE = "./zuul.properties"

        configure do
          raise "No properties file found." unless File.exist?(PROPERTIES_FILE)
          set :properties, JavaProperties::Properties.new(PROPERTIES_FILE)

          db = URI.parse(settings.properties[:database_url])
          ActiveRecord::Base.establish_connection(
            adapter: case db.scheme
                     when "mysql" then
                       "mysql2"
                     when "postgres" then
                       "postgresql"
                     else
                       db.scheme
                     end,
            host: db.host,
            username: db.user,
            password: db.password,
            database: db.path[1..-1],
            encoding: "utf8",
            pool: 20
          )

          @@geoip = GeoIP.new(settings.properties[:geoip_data_file])

          Dir.glob(File.join(File.dirname(__FILE__), "models", "*.rb")).each { |file| require file }
        end

      end
    end
  end
end