require_relative "../default_options"

class CreateRefreshTokens < ActiveRecord::Migration
  def change
    create_table :refresh_tokens, options: default_create_table_options do |t|
      t.timestamps
      t.integer :user_id
      t.integer :client_id
      t.string :token, limit: 50 # BUGBUG: Can this column use case-sensitive collation?
      t.datetime :expires_at
      t.boolean :revoked
    end
    add_index :refresh_tokens, :token, unique: true
  end
end