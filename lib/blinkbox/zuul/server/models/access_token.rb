module Blinkbox::Zuul::Server
  class AccessToken < ActiveRecord::Base

    LIFETIME_IN_SECONDS = 1800
    LIFETIME_IN_DAYS = LIFETIME_IN_SECONDS / (24.0 * 3600.0)

    belongs_to :refresh_token
    
    validates :expires_at, presence: true

    after_initialize do
      self.expires_at = DateTime.now + LIFETIME_IN_DAYS
    end
    
  end
end