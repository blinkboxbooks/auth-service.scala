require "active_model/validator"
require_relative "../../../../blinkbox/zuul/server/models/client"


class UserClientsValidator < ActiveModel::Validator

  def validate(record)
    return if record.deregistered # this will always mean we stay at the same number of registered clients or less
    maximum = options[:maximum]
    @@max_clients_error_message = "Max clients (#{maximum}) already registered"
    if record.user.registered_clients.count >= maximum
      record.errors[:base] << @@max_clients_error_message
    end
  end

  def self.max_clients_error_message
    @@max_clients_error_message
  end

end