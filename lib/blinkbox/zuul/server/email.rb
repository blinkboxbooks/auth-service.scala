require "bunny"
require "nokogiri"
require "securerandom"

module Blinkbox
  module Zuul
    module Server
      module Email

        def self.password_reset(user, link, token)
          send_message("password_reset", user, salutation: user.first_name, resetLink: link, resetToken: token)
        end

        private

        def self.send_message(template, *to, variables)
          builder = Nokogiri::XML::Builder.new(encoding: "utf-8") do |xml|
            xml.sendEmail("xmlns" => "http://schemas.blinkbox.com/books/emails/sending/v1",
                          "xmlns:r" => "http://schemas.blinkbox.com/books/routing/v1",
                          "r:originator" => "zuul",
                          "r:instance" => Socket.gethostname,
                          "r:messageId" => SecureRandom.uuid) {
              xml.template template
              xml.to {
                to.each do |user|
                  xml.recipient { xml.name user.name; xml.email user.username }
                end
              }
              xml.templateVariables {
                variables.each do |key, value|
                  xml.templateVariable { xml.key key; xml.value value }
                end
              }
            }
          end
          enqueue(builder.to_xml)
        end

        def self.enqueue(message)
          amqp_exchange.publish(message, persistent: true, nowait: false)
        end

        def self.amqp_exchange
          unless defined?(@amqp)
            @amqp = { connection: Bunny.new(App.properties[:amqp_server_url]).start }
            @amqp[:channel] = @amqp[:connection].create_channel
            @amqp[:exchange] = @amqp[:channel].fanout("Emails.Outbound", durable: true)
          end
          @amqp[:exchange]
        end

      end
    end
  end
end