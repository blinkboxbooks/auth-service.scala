require_relative "../default_options"

class CreateUserRoles < ActiveRecord::Migration
  def change
    create_table :user_roles, options: default_create_table_options do |t|
      t.string :name, null: false, limit: 3
      t.string :description, limit: 255
    end
    add_index :user_roles, :name

    create_table :user_privileges, id: false, options: default_create_table_options do |t|
      t.datetime :created_at
      t.belongs_to :user
      t.belongs_to :user_role
    end
    add_index :user_privileges, :user_id
    add_index :user_privileges, :user_role_id
  end
end