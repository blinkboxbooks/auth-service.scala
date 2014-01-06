require_relative "../validators/user_client_validator"

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

    validates :name, length: { within: 1..50 }, presence: true
    validates :brand, length: { within: 1..50 }, presence: true
    validates :model, length: { within: 1..50 }, presence: true
    validates :os, length: { within: 1..50 }, presence: true
    validates :user, presence: true
    validates :client_secret, presence: true
    validates_with UserClientsValidator, maximum: MAX_CLIENTS_PER_USER

    returns_value_or_default :name, "Unnamed Client"
    returns_value_or_default :brand, "Unknown Brand"
    returns_value_or_default :model, "Unknown Model"
    returns_value_or_default :os, "Unknown OS"

    after_create :report_client_created
    around_update :report_client_updated

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

    def deregister
      Client.transaction do
        RefreshToken.transaction do
          self.deregistered = true
          if self.refresh_token
            self.refresh_token.revoked = true
            self.refresh_token.save!
          end
          self.save!
          report_client_deregistered
        end
      end
    end

    private

    def report_client_created
      fields = %w{id name brand model os}
      client = fields.each_with_object({}) do |field, hash|
        hash[field] = self[field]
      end
      Blinkbox::Zuul::Server::Reporting.client_registered(self["user_id"], client)
    end

    def report_client_updated
      fields = %w{name brand model os}
      affected_fields = self.changes.keys & fields
      if affected_fields.any?
        old_client, new_client = {}, {}
        fields.each do |field|
          old_client[field] = self.changes[field][0] rescue self[field]
          new_client[field] = self.changes[field][1] rescue self[field]
        end
        yield # saves
        Blinkbox::Zuul::Server::Reporting.client_updated(self["user_id"], self["id"], old_client, new_client)
      else
        yield
      end
    end

    def report_client_deregistered
      fields = %w{id name brand model os}
      client = fields.each_with_object({}) do |field, hash|
        hash[field] = self[field]
      end
      Blinkbox::Zuul::Server::Reporting.client_deregistered(self["user_id"], client)
    end

  end
end
