class RefreshToken < ActiveRecord::Base
  
  belongs_to :user
  belongs_to :device
  has_one :access_token, dependent: :destroy

  validates :token, length: { within: 30..50 }, uniqueness: true
  validates :expires_at, presence: true

  def age
    Time.now - self.updated_at
  end

end