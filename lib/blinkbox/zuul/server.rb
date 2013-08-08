module Blinkbox
  module Zuul
    module Server
      VERSION = "0.0.1"
      COMMIT  = `git rev-parse HEAD 2> /dev/null`.strip
    end
  end
end

require "blinkbox/zuul/server/app"