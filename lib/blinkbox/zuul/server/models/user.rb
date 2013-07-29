module Blinkbox::Zuul::Server
  class User < ActiveRecord::Base

    MIN_PASSWORD_LENGTH = 6

    has_many :refresh_tokens
    has_many :clients

    validates :first_name, length: { within: 1..50 }
    validates :last_name, length: { within: 1..50 }
    validates :username, format: { with: /\A[^@]+@[^@]+\.[^@\.]+\Z/ }, uniqueness: true
    validates :allow_marketing_communications, inclusion: { :in => [true, false] }
    validate :validate_password

    def password=(password)  
      if password
        @password_length = password.length
        self.password_hash = SCrypt::Password.create(password, max_time: 0.2, max_mem: 16 * 1024 * 1024)
      else
        @password_length = 0
        self.password_hash = ""
      end
    end

    def self.authenticate(username, password)
      return nil if username.nil? || password.nil?
      user = User.find_by_username(username)
      if user && SCrypt::Password.new(user.password_hash) == password then user else nil end
    end

    private 

    def validate_password
      if @password_length.nil? || @password_length < MIN_PASSWORD_LENGTH
        errors.add(:password, "is too short (minimum is #{MIN_PASSWORD_LENGTH} characters)")
      end
    end

  end
end