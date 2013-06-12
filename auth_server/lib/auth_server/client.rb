class Client < ActiveRecord::Base

  belongs_to :user
  has_one :refresh_token, dependent: :destroy

  validates :name, length: { within: 1..50 }
  validates :user, presence: true
  validates :client_secret, presence: true
  validates :registration_access_token, presence: true, uniqueness: true
  
end