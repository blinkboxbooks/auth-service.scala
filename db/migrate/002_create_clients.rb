require_relative "../default_options"

class CreateClients < ActiveRecord::Migration

  def self.up
    create_table :clients, options: default_create_table_options do |t|
      t.timestamps
      t.integer :user_id
      t.string :name, limit: 50
      t.string :client_secret, limit: 50
    end
  end

  def self.down
    drop_table :clients
  end

end