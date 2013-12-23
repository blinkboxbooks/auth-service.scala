require "bunny"
require "nokogiri"
require "time"

module Blinkbox
  module Zuul
    module Server
      module Reporting

        def self.user_registered(user)
          builder = Nokogiri::XML::Builder.new(encoding: "utf-8") do |xml|
            xml.userCreated("xmlns" => event_schema("users", "v1"),
                            "xmlns:r" => routing_schema("v1"),
                            "xmlns:v" => versioning_schema,
                            "r:originator" => "zuul",
                            "v:version" => "1.0") {
              xml.timestamp Time.now.getutc.iso8601
              xml.user {
                xml.id user.id
                xml.username user.username
                xml.firstName user.first_name
                xml.lastName user.last_name
                xml.allowMarketingCommunications user.allow_marketing_communications
              }
            }
          end
          enqueue(builder.to_xml)
        end

        def self.user_updated(old_user, new_user)
          builder = Nokogiri::XML::Builder.new(encoding: "utf-8") do |xml|
            xml.userUpdated("xmlns" => event_schema("users", "v1"),
                            "xmlns:r" => routing_schema("v1"),
                            "xmlns:v" => versioning_schema,
                            "r:originator" => "zuul",
                            "v:version" => "1.0") {
              xml.userId new_user.id
              xml.timestamp Time.now.getutc.iso8601
              xml.oldUser {
                xml.username old_user.username
                xml.firstName old_user.first_name
                xml.lastName old_user.last_name
                xml.allowMarketingCommunications old_user.allow_marketing_communications
              }
              xml.newUser {
                xml.username new_user.username
                xml.firstName new_user.first_name
                xml.lastName new_user.last_name
                xml.allowMarketingCommunications new_user.allow_marketing_communications
              }
            }
          end
          enqueue(builder.to_xml)
        end

        def self.client_registered(client)
          builder = Nokogiri::XML::Builder.new(encoding: "utf-8") do |xml|
            xml.deviceCreated("xmlns" => event_schema("devices", "v1"),
                              "xmlns:r" => routing_schema("v1"),
                              "xmlns:v" => versioning_schema,
                              "r:originator" => "zuul",
                              "v:version" => "1.0") {
              xml.userId client.user_id
              xml.timestamp Time.now.getutc.iso8601
              xml.device {
                xml.id client.id
                xml.name client.name
                xml.brand client.brand
                xml.model client.model
                xml.os client.os
              }
            }
          end
          enqueue(builder.to_xml)
        end

        def self.client_updated(old_device, new_device)
          builder = Nokogiri::XML::Builder.new(encoding: "utf-8") do |xml|
            xml.deviceUpdated("xmlns" => event_schema("devices", "v1"),
                              "xmlns:r" => routing_schema("v1"),
                              "xmlns:v" => versioning_schema,
                              "r:originator" => "zuul",
                              "v:version" => "1.0") {
              xml.userId new_device.user_id
              xml.deviceId new_device.id
              xml.timestamp Time.now.getutc.iso8601
              xml.oldDevice {
                xml.name old_device.name
                xml.brand old_device.brand
                xml.model old_device.model
                xml.os old_device.os
              }
              xml.newDevice {
                xml.name new_device.name
                xml.brand new_device.brand
                xml.model new_device.model
                xml.os new_device.os
              }
            }
          end
          enqueue(builder.to_xml)
        end

        def self.client_deregistered(device)
          builder = Nokogiri::XML::Builder.new(encoding: "utf-8") do |xml|
            xml.deviceDeleted("xmlns" => event_schema("devices", "v1"),
                              "xmlns:r" => routing_schema("v1"),
                              "xmlns:v" => versioning_schema,
                              "r:originator" => "zuul",
                              "v:version" => "1.0") {
              xml.userId device.user_id
              xml.timestamp Time.now.getutc.iso8601
              xml.device {
                xml.id device.id
                xml.name device.name
                xml.brand device.brand
                xml.model device.model
                xml.os device.os
              }
            }
          end
          enqueue(builder.to_xml)
        end

        private

        def self.enqueue(message)
          amqp_exchange.publish(message, persistent: true, nowait: false)
        end

        def self.amqp_exchange
          unless defined?(@amqp)
            @amqp = { connection: Bunny.new(App.properties[:amqp_server_url]).start }
            @amqp[:channel] = @amqp[:connection].create_channel
            @amqp[:exchange] = @amqp[:channel].fanout("Events.Outbound", durable: true)
          end
          @amqp[:exchange]
        end

        def self.event_schema(event, version)
          "http://schemas.blinkboxbooks.com/events/#{event}/#{version}"
        end

        def self.routing_schema(version)
          "http://schemas.blinkboxbooks.com/messaging/routing/#{version}"
        end

        def self.versioning_schema
          "http://schemas.blinkboxbooks.com/messaging/versioning"
        end

      end
    end
  end
end
