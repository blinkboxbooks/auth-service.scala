module Blinkbox::Zuul::Server
  class LoginAttempt < ActiveRecord::Base
    belongs_to :user
  end
end