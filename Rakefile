$: << "./lib" if Dir.pwd == File.dirname(__FILE__)
require "sinatra/activerecord/rake"
require "blinkbox/zuul/server/environment"

task :default => :build
task :build => :test

desc "Runs all tests"
task :test do
  Rake::Task[:spec].invoke
  Rake::Task[:features].invoke
end

desc "Runs all rspec tests"
begin
  require "rspec/core/rake_task"
  RSpec::Core::RakeTask.new(:spec) do |t|
    t.pattern = "spec/**/*_spec.rb"
  end
rescue LoadError
  task :spec do
    $stderr.puts "Please install rspec: `gem install rspec`"
  end
end

desc "Runs all feature tests"
begin
  require "cucumber"
  require "cucumber/rake/task"
  Cucumber::Rake::Task.new(:features) do |t|
    t.cucumber_opts = "--profile #{ENV["PROFILE"]}" if ENV["PROFILE"]
  end
rescue LoadError
  task :features do
    $stderr.puts "Please install cucumber: `gem install cucumber`"
  end
end

namespace :db do
  desc "Migrates the database and outputs the generated DDL to a file"
  task :migrate_with_ddl, :file do |task, args|
    File.open(args[:file] || "migration.sql", "w") do |file|
      ActiveSupport::Notifications.subscribe("sql.active_record") do |*ignored, payload|
        sql = payload[:sql].strip.gsub(/\s+/, " ")
        file << "-- " if sql =~ /^(pragma|select|show)/i
        file << sql
        file << ";\n"
      end
      Rake::Task["db:migrate"].invoke
    end
  end
end