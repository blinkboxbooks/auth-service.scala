Given(/^I have an SSO account registered by (books|movies|music)$/) do |user_scope|
  @me = SSOTestUser.new.generate_details(user_scope)
  @me.register
  expect(last_response.status).to eq(200)
end

When(/^I authenticate onto the books website$/) do
  @me.authenticate_books(@credentials)
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
