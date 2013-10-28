module Blinkbox::Zuul::Server
  class RefreshToken < ActiveRecord::Base

    module Status
      VALID = "VALID"
      INVALID = "INVALID"
      NONE = "NONE"
    end

    module Elevation
      CRITICAL = "CRITICAL"
      ELEVATED = "ELEVATED"
      NONE = "NONE"
    end

    module LifeSpan
      TOKEN_LIFETIME_IN_DAYS = 90.0
      CRITICAL_ELEVATION_LIFETIME_IN_SECONDS = 10.minutes

      NORMAL_ELEVATION_LIFETIME_IN_SECONDS = 1.days
    end


    belongs_to :user
    belongs_to :client

    validates :token, length: { within: 30..50 }, uniqueness: true
    validates :expires_at, presence: true

    after_initialize :extend_lifetime
    after_create :set_initial_critical_elevation

    def extend_lifetime
      self.expires_at = DateTime.now + LifeSpan::TOKEN_LIFETIME_IN_DAYS
    end

    def elevation
      if self.critical_elevation_expires_at.future?
        Elevation::CRITICAL
      elsif self.elevation_expires_at.future?
        Elevation::ELEVATED
      else
        Elevation::NONE
      end
    end

    # Returns a boolean representing whether the refresh token is not
    # elevated (true) or elevated/critically elevated (false)
    #
    # @return [Boolean] True if the refresh token has no elevation, i.e.,
    #                   has neither critically elevated nor elevated status.
    def elevation_none?
      elevation == Elevation::NONE
    end

    def extend_elevation_time

      case self.elevation
      when Elevation::CRITICAL
        self.critical_elevation_expires_at = DateTime.now + LifeSpan::CRITICAL_ELEVATION_LIFETIME_IN_SECONDS
      when Elevation::ELEVATED
        self.elevation_expires_at = DateTime.now + LifeSpan::NORMAL_ELEVATION_LIFETIME_IN_SECONDS
      else
      end

      self.save!

    end

    def status
      self.expires_at.past? || self.revoked ? Status::INVALID : Status::VALID
    end

    private

    def set_initial_critical_elevation
      self.critical_elevation_expires_at = DateTime.now + LifeSpan::CRITICAL_ELEVATION_LIFETIME_IN_SECONDS
      self.elevation_expires_at = DateTime.now + LifeSpan::NORMAL_ELEVATION_LIFETIME_IN_SECONDS
      self.save!
    end
  end
end
