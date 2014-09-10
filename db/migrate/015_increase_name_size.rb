require_relative "../default_options"

class IncreaseNameSize < ActiveRecord::Migration

  def change
    change_column :users, :first_name, :string, limit: 64
    change_column :users, :last_name, :string, limit: 64
  end
end
