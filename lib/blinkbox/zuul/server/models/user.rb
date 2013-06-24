module Blinkbox::Zuul::Server
  class User < ActiveRecord::Base

    has_many :refresh_tokens
    has_many :clients

    validates :first_name, length: { within: 1..50 }
    validates :last_name, length: { within: 1..50 }
    validates :email, format: { with: /.+@.+\..+/ }, uniqueness: true
    validates :password_hash, presence: true

    serialize :roles, Array

    def initialize(*params)
      self.password = params.delete("password")
      super
    end

    def password=(password)  
      self.password_hash = SCrypt::Password.create(password) if password
    end

    def self.authenticate(email, password)
      return nil if email.nil? || password.nil?
      user = User.find_by_email(email)
      if user && SCrypt::Password.new(user.password_hash) == password then user else nil end
    end

  end
end