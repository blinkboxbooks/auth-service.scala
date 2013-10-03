require "nokogiri"
require "securerandom"

module Blinkbox
  module Zuul
    module Server
      module Email
        def self.password_reset(user, link, token)
          builder = Nokogiri::XML::Builder.new(encoding: "utf-8") do |xml|
            xml.sendEmail("xmlns" => "http://schemas.blinkbox.com/books/emails/sending/v1",
                          "xmlns:r" => "http://schemas.blinkbox.com/books/routing/v1",
                          "r:originator" => "zuul",
                          "r:instance" => Socket.gethostname,
                          "r:messageId" => SecureRandom.uuid) {
              xml.template { xml.text "password_reset" }
              xml.to {
                xml.recipient {
                  xml.name { xml.text user.name }
                  xml.email { xml.text user.username }
                }
              }
              xml.templateVariables {
                xml.templateVariable {
                  xml.key { xml.text "salutation" }
                  xml.value { xml.text user.first_name }
                }
                xml.templateVariable {
                  xml.key { xml.text "resetLink" }
                  xml.value { xml.text link }
                }
                xml.templateVariable {
                  xml.key { xml.text "resetToken" }
                  xml.value { xml.text token }
                }
              }
            }
          end
          enqueue(builder.to_xml)
        end

        private

        def self.enqueue(message)

        end
      end
    end
  end
end