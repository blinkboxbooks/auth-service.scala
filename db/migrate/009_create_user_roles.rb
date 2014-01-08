require_relative "../default_options"

class CreateUserRoles < ActiveRecord::Migration
  def change
    create_table :user_roles, id: false, options: default_create_table_options do |t|
      t.integer :user_id, null: false
      t.string :role, null: false, limit: 3
    end
    add_index :user_roles, :user_id
  end
end