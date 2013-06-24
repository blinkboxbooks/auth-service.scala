class CreateUsers < ActiveRecord::Migration

  def self.up
    create_table :users do |t|
      t.timestamps
      t.string :first_name, limit: 50
      t.string :last_name, limit: 50
      t.string :email, limit: 320
      t.string :password_hash, limit: 128
      t.string :roles, limit: 120
    end
    add_index :users, :email, name: 'ix__users__email', unique: true
  end

  def self.down
    drop_table :users
  end

end