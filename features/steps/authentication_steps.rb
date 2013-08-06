
Given(/^I have authenticated with my email address and password$/) do
  use_username_and_password_credentials
  submit_authentication_request
  validate_user_token_response
end

Given(/^I have provided my email address$/) do
  use_username_and_password_credentials
  @credentials.delete("password")
end

Given(/^I have provided my email address and password$/, :use_username_and_password_credentials)

Given(/^I have provided my email address, password and client credentials$/) do
  use_username_and_password_credentials
  include_client_credentials
end

Given(/^the email address is different from the one I used to register$/) do
  @credentials["username"] = random_email
end

Given(/^the email address is in a different case to the one I used to register$/) do
  @credentials["username"] = @me.username.swapcase
end

Given(/^I have not provided my (password|client secret)$/) do |name|
  @credentials.delete(oauth_param_name(name))
end

Given(/^I have not provided my client credentials$/) do
  @credentials.delete("client_id")
  @credentials.delete("client_secret")
end

Given(/^I have provided an incorrect (password|client secret)$/) do |name|
  @credentials[oauth_param_name(name)] = random_password
end

Given(/^I have provided another user's client credentials$/) do
  # TODO: Should this setup of other user/client be moved into 'Given' steps?
  other_user = TestUser.new.generate_details
  other_user.register
  other_client = TestClient.new.generate_details
  other_user.register_client(other_client)
  include_client_credentials(other_client)
end

Given(/^I provide my refresh token?$/, :use_refresh_token_credentials) 

Given(/^I provide my refresh token and client credentials$/) do 
  use_refresh_token_credentials
  include_client_credentials
end

Given(/^I have not provided my refresh token$/) do
  use_refresh_token_credentials
  @credentials.delete("refresh_token")
end

Given(/^I have provided an incorrect refresh token$/) do  
  use_refresh_token_credentials
  @credentials["refresh_token"] = random_password
end

Given(/^I have bound my refresh token to my client$/) do  
  use_refresh_token_credentials
  include_client_credentials
  $zuul.authenticate(@credentials)
  validate_user_token_response(refresh_token: :optional)
end

When(/^I submit the (?:authentication|access token refresh) request$/) do
  $zuul.authenticate(@credentials)
end

Then(/^the response indicates that my (?:credentials are|refresh token is) incorrect$/) do
  expect(last_response.status).to eq(400)
  expect(last_response_json["error"]).to eq("invalid_grant")
end