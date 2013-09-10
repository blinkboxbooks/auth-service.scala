require_relative "../default_options"

class AddCriticalElevationEndTime< ActiveRecord::Migration

  def change
    change_table :refresh_tokens do |t|
      t.datetime :critical_elevation_expires_at
    end
  end
end