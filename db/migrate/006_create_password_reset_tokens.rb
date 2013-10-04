require_relative "../default_options"

class CreatePasswordResetTokens < ActiveRecord::Migration
  def change
    create_table :password_reset_tokens, options: default_create_table_options do |t|
      t.timestamps
      t.integer :user_id, null: false
      t.string :token, null: false, limit: 50 # BUGBUG: Can this column use case-sensitive collation?
      t.datetime :expires_at, null: false
      t.boolean :revoked, null: false, default: false
    end
    add_index :password_reset_tokens, :user_id
    add_index :password_reset_tokens, :token, unique: true
  end
end