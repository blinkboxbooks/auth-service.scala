require "sinatra/base"
require "active_record"
require "uri"

module Blinkbox
  module Zuul
    module Server 
      class App < Sinatra::Base

        configure do  
          db = URI.parse(ENV["DATABASE_URL"] || "sqlite3:///db/auth.db")
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