def obtain_access_and_token_via_username_and_password(user = @me, client = nil)
  use_username_and_password_credentials(user)
  include_client_credentials(client) if client
  user.authenticate(@credentials)
  expect(last_response.status).to eq(200)
end

def obtain_access_and_token_via_refresh_token(user = @me, client = nil)
  use_refresh_token_credentials(user)
  include_client_credentials(client) if client
  user.authenticate(@credentials)
  expect(last_response.status).to eq(200)
end

def ensure_elevation_expires_in(seconds)
  time_delta = 3
  expect(last_response_json["token_elevation_expires_in"]).to be_within(time_delta.seconds).of(seconds)
end