require_relative "../default_options"

class CreateUserRoles < ActiveRecord::Migration

  class User < ActiveRecord::Base
    class Role < ActiveRecord::Base
      has_many :privileges
      has_many :users, through: :privileges
    end
    class Privilege < ActiveRecord::Base
      belongs_to :user
      belongs_to :role, foreign_key: :user_role_id
    end
    has_many :privileges
    has_many :roles, through: :privileges
  end

  def change
    create_table :user_roles, options: default_create_table_options do |t|
      t.string :name, null: false, limit: 3
      t.string :description, limit: 255
    end
    add_index :user_roles, :name

    create_table :user_privileges, options: default_create_table_options do |t|
      t.datetime :created_at
      t.belongs_to :user
      t.belongs_to :user_role
    end
    add_index :user_privileges, :user_id
    add_index :user_privileges, :user_role_id

    reversible do |direction|
      direction.up do
        ActiveRecord::Base.transaction do
          roles = []
          roles << User::Role.create!(name: "emp", description: "blinkbox employee")
          roles << User::Role.create!(name: "ops", description: "IT Operations")
          roles << User::Role.create!(name: "csm", description: "Customer Services Manager")
          roles << User::Role.create!(name: "csr", description: "Customer Services Representative")
          roles << User::Role.create!(name: "ctm", description: "Content Manager")
          
          # create a well-known superuser; password (d41P8YETV7OjU^cufcu0) should be changed on live!
          user = User.create!(username: "tm-books-itops@blinkbox.com", 
                              first_name: "IT Ops",
                              last_name: "SU Account",
                              password_hash: "4000$8$3$9118421608d317a4$a03a217134996e3d1e57642bf35589b332ce1c37f8760c8339d52e741fd2f10d",
                              allow_marketing_communications: false)
          user.roles = roles
          user.save!
        end
      end
      direction.down do
        user = User.where(username: "tm-books-itops@blinkbox.com").first
        user.delete unless user.nil?
        User::Role.delete_all
      end
    end
  end
end