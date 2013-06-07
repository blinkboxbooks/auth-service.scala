class CreateDevices < ActiveRecord::Migration

  def self.up
    create_table :devices do |t|
      t.timestamps
      t.integer :user_id
      t.string :name, limit: 50
      t.binary :client_secret, limit: 32
      t.binary :client_access_token, limit: 32
    end
    add_index :devices, :client_access_token, name: 'ix__devices__client_access_token'
  end

  def self.down
    drop_table :devices
  end

end