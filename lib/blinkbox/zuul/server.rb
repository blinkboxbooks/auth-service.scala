module Blinkbox
  module Zuul
    module Server
      VERSION = open(File.join(File.dirname(__FILE__),'..','..','..','VERSION')).read
      # COMMIT = `git rev-parse HEAD 2> /dev/null`.strip
    end
  end
end

require "blinkbox/zuul/server/app"