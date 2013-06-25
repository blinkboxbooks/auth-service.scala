class CreateClients < ActiveRecord::Migration

  def self.up
    create_table :clients do |t|
      t.timestamps
      t.integer :user_id
      t.string :name, limit: 50
      t.string :client_secret, limit: 50
      t.string :registration_access_token, limit: 50
    end
    add_index :clients, :registration_access_token, unique: true
  end

  def self.down
    remove_index :clients, :registration_access_token
    drop_table :clients
  end

end