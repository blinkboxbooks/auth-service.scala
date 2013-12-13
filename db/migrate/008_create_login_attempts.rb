require_relative "../default_options"

class CreateLoginAttempts < ActiveRecord::Migration
  def change
    create_table :login_attempts, options: default_create_table_options do |t|
      t.timestamps
      t.string :username, null: false, limit: 120
      t.boolean :successful, null: false
    end
    add_index :login_attempts, :username
  end
end