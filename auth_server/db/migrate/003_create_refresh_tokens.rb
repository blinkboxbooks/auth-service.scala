class CreateRefreshTokens < ActiveRecord::Migration

  def self.up
    create_table :refresh_tokens do |t|
      t.timestamps
      t.integer :user_id
      t.integer :client_id
      t.string :token, limit: 50
      t.datetime :expires_at
      t.boolean :revoked
    end
    add_index :refresh_tokens, :token, name: 'ix__refresh_tokens__token', unique: true
  end

  def self.down
    drop_table :refresh_tokens
  end

end