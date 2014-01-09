module Blinkbox::Zuul::Server
  class TooManyRequests < StandardError
    attr_accessor :retry_after
  end
end