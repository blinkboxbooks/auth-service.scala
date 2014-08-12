require_relative "../default_options"

class AddSsoLinked < ActiveRecord::Migration

  def change
    add_column :users, :sso_linked, :boolean, default: false
  end
end
