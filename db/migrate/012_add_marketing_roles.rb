require_relative "../default_options"

class AddMarketingRoles < ActiveRecord::Migration

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
    reversible do |direction|
      direction.up do
        # add the roles
        roles = []
        roles << User::Role.create!(name: "mer", description: "Merchandising")
        roles << User::Role.create!(name: "mkt", description: "Marketing")
        
        # update the well-known superuser to have the role
        user = User.where(username: "tm-books-itops@blinkbox.com").first
        user.roles = roles
        user.save!
      end
      direction.down do
        %w(mer mkt).each do |role_name|
          role = User::Role.where(name: role_name).first
          User::Privilege.where(role: role).each do |privilege|
            User::Privilege.destroy(privilege)
          end
          User::Role.destroy(role)
        end
      end
    end
  end

end