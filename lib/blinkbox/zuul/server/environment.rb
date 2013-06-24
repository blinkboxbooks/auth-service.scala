require "sinatra/base"
require "active_record"
require "uri"

module Blinkbox
  class Zuul
    class Server < Sinatra::Base

      configure do  
        db = URI.parse(ENV['DATABASE_URL'] || "sqlite3:///db/auth.db")
        ActiveRecord::Base.establish_connection(
          :adapter  => db.scheme == 'postgres' ? 'postgresql' : db.scheme,
          :host     => db.host,
          :username => db.user,
          :password => db.password,
          :database => db.path[1..-1],
          :encoding => 'utf8',
          :pool     => 20
        )
      end

    end
  end
end