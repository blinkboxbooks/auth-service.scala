module Blinkbox::Zuul::Server
  class Client < ActiveRecord::Base

    class << self
      private
      def returns_value_or_default(attribute, default)
        define_method attribute do
          read_attribute(attribute) || default
        end
      end
    end

    MAX_CLIENTS_PER_USER = 12

    belongs_to :user
    has_one :refresh_token

    validates :name, length: { within: 1..50 }, allow_nil: true
    validates :brand, length: { within: 1..50 }, allow_nil: true
    validates :model, length: { within: 1..50 }, allow_nil: true
    validates :os, length: { within: 1..50 }, allow_nil: true
    validates :user, presence: true
    validates :client_secret, presence: true

    returns_value_or_default :name, "Unnamed Client"
    returns_value_or_default :brand, "Unknown Brand"
    returns_value_or_default :model, "Unknown Model"
    returns_value_or_default :os, "Unknown OS"

    def self.authenticate(id, secret)
      return nil if id.nil? || secret.nil?
      /\Aurn:blinkbox:zuul:client:(?<numeric_id>\d+)\Z/ =~ id
      client = Client.find_by_id(numeric_id.to_i) if numeric_id
      if client && client.client_secret == secret && !client.deregistered then
        client
      else
        nil
      end
    end

  end
end