require_relative "../default_options"

class AddRoleConstraints < ActiveRecord::Migration

  def change
    # fix the index on user_roles.name to be unique
    remove_index :user_roles, column: :name
    add_index :user_roles, :name, unique: true

    reversible do |direction|
      direction.up do
        # fix privileges to require a user and a role
        change_column :user_privileges, :user_id, :int, null: :false
        change_column :user_privileges, :user_role_id, :int, null: :false
      end
      direction.down do
        # un-fix privileges to require a user and a role
        change_column :user_privileges, :user_id, :int, null: :true
        change_column :user_privileges, :user_role_id, :int, null: :true
      end
    end
  end

end