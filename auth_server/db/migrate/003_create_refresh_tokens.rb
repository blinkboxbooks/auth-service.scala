class CreateRefreshTokens < ActiveRecord::Migration

  def self.up
    create_table :refresh_tokens do |t|
      t.timestamps
      t.integer :user_id
      t.integer :device_id
      t.binary :token, limit: 32
      t.datetime :expires_at
    end
    add_index :refresh_tokens, :token, name: 'ix__refresh_tokens__token'
  end

  def self.down
    drop_table :refresh_tokens
  end

end