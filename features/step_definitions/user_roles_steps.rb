Given(/^I am authenticated as a user in the "employee" and "content manager" roles$/) do
  # yes, this is a bit crap, but until we have role management it's about as good as we can do...
  @me = TestUser.new
  @me.username = "tm-books-itops@blinkbox.com"
  @me.password = "d41P8YETV7OjU^cufcu0"
  obtain_access_and_token_via_username_and_password
end

Then(/^it does not contain any role information$/) do
  expect(last_response_json["user_roles"]).to be_empty
end

Then(/^it indicates that I am in the "(.*?)" role$/) do |role_name|
  expect(last_response_json["user_roles"]).to include(role_token(role_name))
end