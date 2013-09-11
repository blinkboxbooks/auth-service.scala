Given(/^I have bound my tokens to my client$/) do
  use_refresh_token_credentials
  include_client_credentials
  @me.authenticate(@credentials)
  expect(last_response.status).to eq(200)
end

When(/^I provide my email address( and password)?$/) do |with_password|
  use_username_and_password_credentials
  @credentials.delete("password") unless with_password
end

When(/^I provide my email address, password and client credentials$/) do
  use_username_and_password_credentials
  include_client_credentials
end

When(/^the email address is different from the one I used to register$/) do
  @credentials["username"] = random_email
end

When(/^the email address is in a different case to the one I used to register$/) do
  @credentials["username"] = @me.username.swapcase
end

When(/^I do not provide my (password|client secret)$/) do |name|
  @credentials.delete(oauth_param_name(name))
end

When(/^the (password|client secret) is incorrect$/) do |name|
  @credentials[oauth_param_name(name)] = random_password
end

When(/^I do not provide my client credentials$/) do
  @credentials.delete("client_id")
  @credentials.delete("client_secret")
end

When(/^I provide the other user's client credentials$/) do
  include_client_credentials(@your_client)
end

When(/^I provide my refresh token( and client credentials)?$/) do |with_client_credentials|
  use_refresh_token_credentials
  include_client_credentials if with_client_credentials
end

When(/^I do not provide my refresh token$/) do
  use_refresh_token_credentials
  @credentials.delete("refresh_token")
end

When(/^I provide a nonexistent refresh token$/) do
  use_refresh_token_credentials
  @credentials["refresh_token"] = random_password
end

When(/^I submit the (authentication|access token refresh) request$/) do |request_type|
  # The assumption is that you have called a step with "I provide my ..." before you call this as set up to @credentials
  @me.authenticate(@credentials)
end

Then(/^the response indicates that my (?:credentials are|refresh token is) (?:incorrect|invalid)$/) do
  expect(last_response.status).to eq(400)
  authenticate_header = Hash[*last_response['WWW-Authenticate'].scan(/([^\ ]+)="([^\"]+)"/).flatten]
  expect(authenticate_header["error"]).to eq("invalid_grant")
end