module Blinkbox::Zuul::Server
  class Client < ActiveRecord::Base

    MAX_CLIENTS_PER_USER = 12

    belongs_to :user
    has_one :refresh_token

    validates :name, length: { within: 1..50 }
    validates :model, length: { within: 1..50 }
    validates :user, presence: true
    validates :client_secret, presence: true

    # TODO: Should probably hash the client secret...
    def self.authenticate(id, secret)
      return nil if id.nil? || secret.nil?
      numeric_id = id.match(/\Aurn:blinkbox:zuul:client:(\d+)\Z/)[1]
      client = Client.find_by_id(numeric_id.to_i) if numeric_id
      if client && client.client_secret == secret then client else nil end
    end
    
  end
end