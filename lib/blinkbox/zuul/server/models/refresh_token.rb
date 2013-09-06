module Blinkbox::Zuul::Server
  class RefreshToken < ActiveRecord::Base

    module Status
      VALID = "VALID"
      INVALID = "INVALID"
    end

    module Elevation
      CRITICAL = "CRITICAL"
      NONE = "NONE"
    end

    module LifeSpan
      TOKEN_LIFETIME_IN_DAYS = 90.0
      ELEVATION_LIFETIME_IN_SECONDS = if ENV["ELEVATION_TIMESPAN"]
                                        10.send(ENV["ELEVATION_TIMESPAN"])
                                      else
                                        10.minutes
                                      end
    end


    belongs_to :user
    belongs_to :client
    has_one :access_token

    validates :token, length: { within: 30..50 }, uniqueness: true
    validates :expires_at, presence: true

    after_initialize :extend_lifetime, :extend_elevation_lifetime

    def extend_lifetime
      self.expires_at = DateTime.now + RefreshToken::LifeSpan::TOKEN_LIFETIME_IN_DAYS
    end

    def extend_elevation_lifetime
      self.status = RefreshToken::Status::VALID
      self.elevation = RefreshToken::Elevation::CRITICAL
      self.elevation_expires_at = DateTime.now + RefreshToken::LifeSpan::ELEVATION_LIFETIME_IN_SECONDS
    end

  end
end