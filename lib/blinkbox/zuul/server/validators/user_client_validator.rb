require "active_model/validator"
require_relative "../../../../blinkbox/zuul/server/models/client"


class UserClientsValidator < ActiveModel::Validator
  @@message = ""

  def validate(record)
    return if record.deregistered # this will always mean we stay at the same number of registered clients or less
    if record.user.registered_clients.count >= Blinkbox::Zuul::Server::Client::MAX_CLIENTS_PER_USER
      @@message = "Max clients (#{Blinkbox::Zuul::Server::Client::MAX_CLIENTS_PER_USER}) already registered"
      record.errors[:base] << @@message
    end
  end

  def self.message
    return @@message
  end
end