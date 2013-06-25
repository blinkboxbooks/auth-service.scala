$: << "./lib" if Dir.pwd == File.dirname(__FILE__)
require "sinatra/activerecord/rake"
require "bundler/gem_tasks"
require "rake/db/migrate_sql"
require "blinkbox/zuul/server/environment"

namespace :db do
  task :migrate_sql, :file do |task, args|
    File.open(args[:file] || "output.sql", "w") do |file|
      ActiveRecord::Base.connection.send_sql_to file
      Rake::Task["db:migrate"].invoke
    end
  end
end