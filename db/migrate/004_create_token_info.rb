require_relative "../default_options"

class CreateTokenInfo < ActiveRecord::Migration

  def change
     change_table :refresh_tokens do |t|
      t.string :status
      t.string :elevation
      t.datetime :elevation_expires_at
    end
  end
end