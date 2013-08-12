module Blinkbox::Zuul::Server
  class RefreshToken < ActiveRecord::Base

    LIFETIME_IN_DAYS = 90.0

    belongs_to :user
    belongs_to :client
    has_one :access_token

    validates :token, length: { within: 30..50 }, uniqueness: true
    validates :expires_at, presence: true

    after_initialize :extend_lifetime

    def extend_lifetime
      self.expires_at = DateTime.now + LIFETIME_IN_DAYS
    end

  end
end