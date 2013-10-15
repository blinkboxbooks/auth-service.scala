require_relative "../default_options"

class AddBrandAndOsToClients < ActiveRecord::Migration

  class Client < ActiveRecord::Base; end

  def change
    change_table :clients do |t|
      t.string :brand, limit: 50
      t.string :os, limit: 50
    end
    Client.where(name: "Unnamed Client").update_all(name: nil)
    Client.where(model: "Unknown Device").update_all(model: nil)
  end

end