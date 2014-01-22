require_relative "../default_options"

class CreateUserPreviousUsernames < ActiveRecord::Migration

  class User < ActiveRecord::Base
    class PreviousUsername < ActiveRecord::Base
      belongs_to :user
    end
    has_many :previous_usernames
  end

  def change
    create_table :user_previous_usernames, options: default_create_table_options do |t|
      t.datetime :created_at
      t.belongs_to :user
      t.string :username, null: false, limit: 120
    end
    add_index :user_previous_usernames, :user_id
    add_index :user_previous_usernames, :username
  end
end