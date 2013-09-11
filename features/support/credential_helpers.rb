def use_username_and_password_credentials(user = @me)
  @credentials = {
    "grant_type" => "password",
    "username" => user.username,
    "password" => user.password
  }
end

def use_refresh_token_credentials(user = @me)
  @credentials = {
    "grant_type" => "refresh_token",
    "refresh_token" => user.refresh_token
  }
end

def include_client_credentials(client = @my_client)
  expect(@credentials).to_not be_nil
  @credentials["client_id"] = client.id
  @credentials["client_secret"] = client.secret
end