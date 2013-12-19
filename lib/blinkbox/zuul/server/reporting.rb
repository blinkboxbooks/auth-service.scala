require "bunny"
require "nokogiri"

module Blinkbox
  module Zuul
    module Server
      module Reporting

        //

        private

        def self.enqueue(message)
          amqp_exchange.publish(message, persistent: true, nowait: false)
        end

        def self.amqp_exchange
          unless defined?(@amqp)
            @amqp = { connection: Bunny.new(App.properties[:amqp_server_url]).start }
            @amqp[:channel] = @amqp[:connection].create_channel
            @amqp[:exchange] = @amqp[:channel].fanout("", durable: true)
          end
          @amqp[:exchange]
        end

      end
    end
  end
end
