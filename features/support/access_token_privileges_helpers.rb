def obtain_access_and_token
  use_username_and_password_credentials
  @me.authenticate(@credentials)
  expect(last_response.status).to eq(200)
end