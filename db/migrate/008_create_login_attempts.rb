require_relative "../default_options"

class CreateLoginAttempts < ActiveRecord::Migration
  def change
    create_table :login_attempts, options: default_create_table_options do |t|
      t.datetime :created_at, null: false
      t.string :username, null: false, limit: 120
      t.boolean :successful, null: false
      t.string :client_ip, limit: 15
    end
    add_index :login_attempts, :username
  end
end