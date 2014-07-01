GC::Profiler.enable

$LOAD_PATH << "./lib"
require "blinkbox/zuul/server"

profile_dir = ENV["PROFILE"]
if profile_dir
  require "ruby-prof"
  Dir.mkdir(profile_dir) unless Dir.exist?(profile_dir)
  use Rack::RubyProf, path: profile_dir
end

run Blinkbox::Zuul::Server::App.new