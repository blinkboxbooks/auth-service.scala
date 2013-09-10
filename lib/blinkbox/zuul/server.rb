module Blinkbox
  module Zuul
    module Server
      VERSION = open(File.join(File.dirname(__FILE__),'..','..','..','VERSION')) do |f|
        f.read
      end
    end
  end
end

require "blinkbox/zuul/server/app"