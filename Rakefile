$: << "./lib" if Dir.pwd == File.dirname(__FILE__)
require "sinatra/activerecord/rake"
require "blinkbox/zuul/server/environment"
require "cucumber"
require "cucumber/rake/task"
require "rspec/core/rake_task"

task :default => :build
task :build => :test

task :test do
  Rake::Task['spec'].invoke
  Rake::Task['features'].invoke
end

RSpec::Core::RakeTask.new(:spec) do |t|
  t.pattern = "spec/**/*_spec.rb"
end

Cucumber::Rake::Task.new(:features) do |t|
  t.cucumber_opts = "--profile #{ENV["PROFILE"]}" if ENV["PROFILE"]
end

namespace :db do
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