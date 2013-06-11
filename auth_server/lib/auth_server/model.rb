require "active_record"

class User < ActiveRecord::Base
  has_many :refresh_tokens
  has_many :devices

  validates :first_name, length: { within: 1..50 }
  validates :last_name, length: { within: 1..50 }
  validates :email, format: { with: /.+@.+\..+/ }, uniqueness: true
  validates :password_hash, presence: true
end

class Device < ActiveRecord::Base
  belongs_to :user
  has_one :refresh_token, dependent: :destroy

  validates :name, length: { within: 1..50 }
  validates :client_secret, presence: true
  validates :client_access_token, presence: true, uniqueness: true
end

class RefreshToken < ActiveRecord::Base
  belongs_to :user
  belongs_to :device
  has_one :access_token, dependent: :destroy

  validates :token, length: { within: 30..50 }, uniqueness: true
  validates :expires_at, presence: true
end

class AccessToken < ActiveRecord::Base
  belongs_to :refresh_token
  
  validates :expires_at, presence: true
end