require_relative "../default_options"

class RemoveElevationField < ActiveRecord::Migration
  def up
    remove_column :refresh_tokens, :elevation
  end

  def down

  end
end