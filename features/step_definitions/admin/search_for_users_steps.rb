Given(/^I am authenticated as a user in the "(.*?)" role$/) do |role_name|
  # yes, this is a bit crap, but until we have role management it's about as good as we can do...
  @me = TestUser.new
  @me.username = "tm-books-itops@blinkbox.com"
  @me.password = "d41P8YETV7OjU^cufcu0"
  obtain_access_and_token_via_username_and_password
end

Given(/^there is a registered user with the attributes:$/) do |table|
  user = TestUser.new.generate_details
  table.attribute_hash(:snake_case).each do |key, val|
    user.send("#{key}=", val)
  end
  response = user.register
  expect(last_response_json["error_reason"]).to eq("username_already_taken") unless response.status == 200
end

When(/^I search for users with username "(.*?)"$/) do |username|
  $zuul.admin_find_user({ username: username }, @me.access_token)
end

Then(/^the response contains a list containing one user$/) do
  expect(last_response.status).to eq(200)
  expect(last_response_json.count).to eq(1)
end