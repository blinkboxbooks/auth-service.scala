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
      CRITICAL_ELEVATION_LIFETIME_IN_SECONDS = if ENV["ELEVATION_TIMESPAN"]
                                                 10.send(ENV["ELEVATION_TIMESPAN"])
                                               else
                                                 10.minutes
                                               end

      NORMAL_ELEVATION_LIFETIME_IN_SECONDS = 1.days
    end


    belongs_to :user
    belongs_to :client
    has_one :access_token

    validates :token, length: {within: 30..50}, uniqueness: true
    validates :expires_at, presence: true

    after_initialize :extend_lifetime
    after_create :extend_critical_elevation_lifetime

    def extend_lifetime
      self.expires_at = DateTime.now + LifeSpan::TOKEN_LIFETIME_IN_DAYS
    end

    def extend_elevation_time
      update_elevation

      case self.elevation
        when Elevation::CRITICAL
          self.critical_elevation_expires_at = DateTime.now + LifeSpan::CRITICAL_ELEVATION_LIFETIME_IN_SECONDS
        when Elevation::ELEVATED
          self.elevation_expires_at = DateTime.now + LifeSpan::NORMAL_ELEVATION_LIFETIME_IN_SECONDS
        else
      end

      self.save!

    end

    def update_elevation
      if self.critical_elevation_expires_at.future?
        self.elevation = Elevation::CRITICAL
      elsif self.elevation_expires_at.future?
        self.elevation = Elevation::ELEVATED
      elsif self.expires_at.past?
        self.elevation = Elevation::NONE
        self.status = Status::INVALID
      else
        self.elevation = Elevation::NONE
      end

      self.save!
    end

    private

    def extend_critical_elevation_lifetime
      self.status = RefreshToken::Status::VALID
      self.critical_elevation_expires_at = DateTime.now + LifeSpan::CRITICAL_ELEVATION_LIFETIME_IN_SECONDS
      self.elevation_expires_at = DateTime.now + LifeSpan::NORMAL_ELEVATION_LIFETIME_IN_SECONDS
      self.elevation = RefreshToken::Elevation::CRITICAL
      self.save!
    end
  end
end