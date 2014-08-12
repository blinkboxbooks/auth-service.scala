require_relative "../default_options"

class AddSsoId < ActiveRecord::Migration

  def change
    add_column :users, :sso_id, :string, default: nil
  end
end
