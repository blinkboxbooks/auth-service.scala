module Blinkbox::Zuul::Server
  class User < ActiveRecord::Base
    class TooManyAttempts < StandardError
      attr_accessor :retry_after
    end

    MIN_PASSWORD_LENGTH = 6

    has_many :refresh_tokens
    has_many :clients
    has_many :password_reset_tokens
    has_many :login_attempts

    validates :first_name, length: { within: 1..50 }
    validates :last_name, length: { within: 1..50 }
    validates :username, format: { with: /\A[^@]+@([^@\.]+\.)+[^@\.]+\Z/ }, uniqueness: true
    validates :allow_marketing_communications, inclusion: { :in => [true, false] }
    validates :password_hash, presence: true
    validate :validate_password

    def name
      "#{first_name} #{last_name}"
    end

    def password=(password)
      if password
        @password_length = password.length
        self.password_hash = SCrypt::Password.create(password, max_time: 0.2, max_mem: 16 * 1024 * 1024)
      else
        @password_length = 0
        self.password_hash = ""
      end
    end

    def registered_clients
      clients.select { |client| !client.deregistered }
    end

    def self.authenticate(username, password, client_ip)
      return nil if username.nil? || password.nil?
      throttle_login_attempts(username)
      user = User.find_by_username(username)
      successful = user != nil && SCrypt::Password.new(user.password_hash) == password
      LoginAttempt.new(username: username, successful: successful, client_ip: client_ip).save!
      successful ? user : nil
    end

    private

    def validate_password
      # this validation is only needed when the password is changed, in which case we recorded
      # the length of the new password in the accessor. if the password length attribute is nil 
      # then the password hasn't been changed and we don't need to do the validation.
      if @password_length && @password_length < MIN_PASSWORD_LENGTH
        errors.add(:password, "is too short (minimum is #{MIN_PASSWORD_LENGTH} characters)")
      end
    end

    def self.throttle_login_attempts(username)
      recent_attempts = LoginAttempt.where(username: username).limit(5).order(id: :desc)
      failed_attempts = recent_attempts.take_while { |attempt| !attempt.successful? }
      if failed_attempts.count == 5
        period = Time.now - recent_attempts.last.created_at
        if period < 15
          error = TooManyAttempts.new("Too many incorrect password attempts; try again later")
          error.retry_after = (15 - period).to_i.to_s
          raise error
        end
      end
    end

  end
end