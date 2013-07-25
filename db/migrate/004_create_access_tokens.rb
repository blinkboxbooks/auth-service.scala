require_relative "../default_options"

class CreateAccessTokens < ActiveRecord::Migration

  def self.up
    create_table :access_tokens, options: default_create_table_options do |t|
      t.timestamps
      t.integer :refresh_token_id
      t.datetime :expires_at
    end
  end

  def self.down
    drop_table :access_tokens
  end

end