require_relative "../default_options"

class CreateUsers < ActiveRecord::Migration

  def self.up
    create_table :users, options: default_create_table_options do |t|
      t.timestamps
      t.string :username, limit: 120
      t.string :first_name, limit: 50
      t.string :last_name, limit: 50
      t.string :password_hash, limit: 128
      t.boolean :allow_marketing_communications
    end
    add_index :users, :username, unique: true
  end

  def self.down
    remove_index :users, :username
    drop_table :users
  end

end