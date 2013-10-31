require "active_model/validator"
require_relative "../../../../blinkbox/zuul/server/models/client"


class UserClientsValidator < ActiveModel::Validator
  MAX_CLIENTS_PER_USER = 12
  MAX_CLIENTS_ERROR_MESSAGE = "Max clients (#{MAX_CLIENTS_PER_USER}) already registered"

  def validate(record)
    return if record.deregistered # this will always mean we stay at the same number of registered clients or less
    if record.user.registered_clients.count >= MAX_CLIENTS_PER_USER
      record.errors[:base] << MAX_CLIENTS_ERROR_MESSAGE
    end
  end

end