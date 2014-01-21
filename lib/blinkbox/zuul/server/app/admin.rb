module Blinkbox::Zuul::Server
  class App < Sinatra::Base

    require_user_authorization_for %r{^/admin}, roles: %w(csm csr ops)
    
    namespace "/admin" do

      get "/users", provides: :json do
        if params["username"].present?
          users = User.where_has_had_username(params["username"])
        elsif params["first_name"].present? && params["last_name"].present?
          users = User.where(first_name: params["first_name"], last_name: params["last_name"])
        elsif params["user_id"].present?
          numeric_id = /\d+$/.match(params["user_id"])[0].to_i
          users = User.where(id: numeric_id)
        else
          invalid_request "Not enough arguments supplied"
        end
        { "items" => users }.to_json(format: :admin)
      end

    end
  end
end