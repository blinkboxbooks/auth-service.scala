module Blinkbox::Zuul::Server
  class PasswordResetToken < ActiveRecord::Base

    TOKEN_LIFETIME_IN_DAYS = 1.0

    belongs_to :user

    validates :token, length: { within: 30..50 }, uniqueness: true
    validates :expires_at, presence: true

    after_initialize do
      self.expires_at = DateTime.now + TOKEN_LIFETIME_IN_DAYS
    end

    def expired?
      self.expires_at >= DateTime.now
    end

  end
end