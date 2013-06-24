($LOAD_PATH << File.expand_path("../lib", __FILE__)).uniq!
require "blinkbox/zuul/server/version"

Gem::Specification.new do |s|
  s.name = "blinkbox-zuul-server"
  s.version = Blinkbox::Zuul::Server::VERSION
  s.summary = "blinkbox's authentication server"
  s.description = "An authentication server."
  s.author = "blinkbox"
  s.email = "greg@blinkbox.com"
  s.homepage = "https://git.mobcastdev.com/zuul/zuul-server"
  s.license = "none"

  s.files = `git ls-files`.split($/)
  s.executables = s.files.grep(%r{^bin/}) { |f| File.basename(f) }
  s.test_files = s.files.grep(%r{^(test|spec|features)/})
  s.require_paths = ["lib"]
  s.extra_rdoc_files = ["README.md"]

  s.add_runtime_dependency "activerecord", "~> 3.2.13"
  s.add_runtime_dependency "multi_json", "~> 1.7"
  s.add_runtime_dependency "sandal", "~> 0.5.1"
  s.add_runtime_dependency "scrypt", "~> 1.1.0"
  s.add_runtime_dependency "sinatra", "~> 1.2.2"
  s.add_runtime_dependency "sinatra-activerecord", "~> 1.2.2"

  s.add_development_dependency "bundler"
  s.add_development_dependency "coveralls"
  s.add_development_dependency "cucumber"
  s.add_development_dependency "rake"
  s.add_development_dependency "rspec"
  s.add_development_dependency "simplecov"
  s.add_development_dependency "sqlite3"
  s.add_development_dependency "yard"

  s.requirements << "openssl 1.0.1c for EC signature methods"
end