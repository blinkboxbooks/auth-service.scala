require_relative "../default_options"

class AddElevationInfo < ActiveRecord::Migration

  def change
    change_table :refresh_tokens do |t|
      t.datetime :elevation_expires_at
      t.datetime :critical_elevation_expires_at
    end
  end
end