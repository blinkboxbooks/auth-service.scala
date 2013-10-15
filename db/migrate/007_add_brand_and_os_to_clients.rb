require_relative "../default_options"

class AddBrandAndOsToClients < ActiveRecord::Migration
  def change
    change_table :clients do |t|
      t.string :brand, limit: 50
      t.string :os, limit: 50
    end
  end
end