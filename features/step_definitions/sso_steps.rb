Given(/^I have an SSO account registered by (books|movies|music)$/) do |user_scope|
  @me = SSOTestUser.new.generate_details(user_scope)
  @me.register
  expect(last_response.status).to eq(200)
end

When(/^I authenticate onto the books website$/) do
  @me.authenticate_books(@credentials)
end

When(/^I submit the registration request on the books website$/) do
  @me.register_books
end

When(/^I submit an authentication request to SSO from (books|movies|music)?$/) do |service|
  $sso.authenticate(@credentials, service)
end

When(/^I update my password in SSO?$/) do
  $sso.password_update(@password_params[:new_password], @me.access_token)
  expect(last_response.status).to eq(204)
end

Then(/^I am (not )?able to use my (new|old) password to authenticate to SSO via (books|movies|music)$/) do |negative, password, service|
  use_username_and_password_credentials
  @credentials["password"] = @password_params[:old_password]  if password == "old"
  @credentials["password"] = @password_params[:new_password]  if password == "new"
  $sso.authenticate(@credentials, service)
  verb = negative ? :to_not : :to
  expect(last_response.status).send(verb, eq(200))
end

Then(/^my SSO account is linked to the service I registered with$/) do
  $sso.link(@me.access_token)
end

Then(/^my details are correct in the SSO system$/) do
  @admin = SSOTestUser.new
  @admin.username = 'admin@blinkbox.com'
  @admin.password = 'Bl1nkb0x'
  obtain_access_and_token_via_username_and_password(@admin)
  $sso.admin_find_user({ username: @me.username }, @admin.access_token)
  validate_sso_user_response
  validate_sso_linked_to_books
end
