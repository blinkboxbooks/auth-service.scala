$: << "./lib" if Dir.pwd == File.dirname(__FILE__)
require "sinatra/activerecord/rake"
require "bundler/gem_tasks"
require "blinkbox/zuul/server/environment"

namespace :db do
  task :migrate_with_ddl, :file do |task, args|
    File.open(args[:file] || "migration.sql", "w") do |file|
      ActiveSupport::Notifications.subscribe("sql.active_record") do |*ignored, payload|
        sql = payload[:sql].strip.gsub(/\s+/, " ")
        file << "-- " if sql =~ /^(select|pragma)/i
        file << sql
        file << ";\n"
      end
      Rake::Task["db:migrate"].invoke
    end
  end
end