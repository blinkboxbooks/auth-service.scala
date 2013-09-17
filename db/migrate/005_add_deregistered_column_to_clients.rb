require_relative "../default_options"

class AddDeregisteredColumnToClients < ActiveRecord::Migration

  def change
    change_table :clients do |t|
      t.boolean :deregistered
    end
  end

end