require_relative "../default_options"

class AddBrandAndOsToClients < ActiveRecord::Migration

  class Client < ActiveRecord::Base; end

  def change
    change_table :clients do |t|
      t.string :brand, limit: 50
      t.string :os, limit: 50
    end
    reversible do |direction|
      direction.up do
        Client.where(name: "Unnamed Client").update_all(name: nil)
        Client.where(model: "Unknown Device").update_all(model: nil)
      end
      direction.down do
        Client.where(name: nil).update_all(name: "Unnamed Client")
        Client.where(model: nil).update_all(model: "Unknown Device")
      end
    end
  end

end