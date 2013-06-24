module Blinkbox::Zuul::Server
  class User < ActiveRecord::Base

    MIN_PASSWORD_LENGTH = 6

    has_many :refresh_tokens
    has_many :clients

    validates :first_name, length: { within: 1..50 }
    validates :last_name, length: { within: 1..50 }
    validates :email, format: { with: /.+@.+\..+/ }, uniqueness: true
    validate :validate_password

    serialize :roles, Array

    def initialize(*params)
      self.password = params.delete("password")
      super
    end

    def password=(password)  
      if password
        @password_length = password.length
        self.password_hash = SCrypt::Password.create(password)
      else
        @password_length = 0
        # TODO: This should probably set password_has to nil but that raises an error...
      end
    end

    def self.authenticate(email, password)
      return nil if email.nil? || password.nil?
      user = User.find_by_email(email)
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