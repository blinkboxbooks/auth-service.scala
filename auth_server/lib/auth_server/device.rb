class Device < ActiveRecord::Base
  belongs_to :user
  has_one :refresh_token, dependent: :destroy

  validates :name, length: { within: 1..50 }
  validates :client_secret, presence: true
  validates :client_access_token, presence: true, uniqueness: true
end