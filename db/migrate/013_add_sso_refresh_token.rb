require_relative "../default_options"

class AddSsoRefreshToken < ActiveRecord::Migration

  def change
    add_column :refresh_tokens, :sso_refresh_token, :string, length: 512
  end
end
