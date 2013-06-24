module Blinkbox
  module Zuul
    class Server
      class AccessToken < ActiveRecord::Base

        belongs_to :refresh_token
        
        validates :expires_at, presence: true
        
      end
    end
  end
end