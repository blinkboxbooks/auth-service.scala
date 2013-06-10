require "active_record"

class User < ActiveRecord::Base
  has_many :refresh_tokens
  has_many :devices

  validates :first_name, length: { within: 1..50 }
  validates :last_name, length: { within: 1..50 }
  validates :email, format: { with: /.+@.+\..+/ }
  validates :password_hash, length: { within: 64..128 }
end

class RefreshToken < ActiveRecord::Base
  belongs_to :user
  belongs_to :device
  has_one :access_token, dependent: :destroy

  validates :token, length: { within: 30..50 }
end

class AccessToken < ActiveRecord::Base
  belongs_to :refresh_token
end

class Device < ActiveRecord::Base
  belongs_to :user
  has_one :refresh_token, dependent: :destroy

  validates :name, length: { within: 1..50 }
  validates :client_secret, length: { is: 32 }
  validates :client_access_token, length: { within: 30..50 }
end