module Blinkbox::Zuul::Server
  class App < Sinatra::Base
    namespace "/admin" do

      get "/users", provides: :json do
        users = if params["username"]
                  User.where(username: params["username"])
                elsif params["first_name"] && params["last_name"]
                  User.where(first_name: params["first_name"], last_name: params["last_name"])
                elsif params["user_id"]
                  id = /\d+$/.match(params["user_id"])[0].to_i
                  User.where(id: id)
                end
        { "items" => users }.to_json(format: :admin)
      end

    end
  end
end