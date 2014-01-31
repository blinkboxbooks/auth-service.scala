Given(/^there is a registered user, call (?:him|her|them) "(\w+)"$/) do |user_handle|
  register_random_user(user_handle)
end

Given(/^there is a registered user, call (?:him|her|them) "(\w+)", who has previously changed (?:his|her|their) email address(?: (#{CAPTURE_INTEGER}) times)?$/) do |user_handle, n|
  n ||= 1
  user = register_random_user(user_handle)  
  n.times do
    sleep(1)
    user.username = random_email
    $zuul.update_user(user, user.access_token)
  end
end

Given(/^there is a registered user, call (?:him|her|them) "(\w+)", who registered with (\w+)'s old email address$/) do |user_handle, other_handle|
  other_user = known_user(other_handle)
  user = register_random_user(user_handle, username: other_user.previous_usernames.last)
end

When(/^I search for users with (\w+)'s email address$/) do |user_handle|
  user = known_user(user_handle)
  $zuul.admin_find_user({ username: user.username }, @me.access_token)
end

When(/^I search for users with (\w+)'s old email address$/) do |user_handle|
  user = known_user(user_handle)
  $zuul.admin_find_user({ username: user.previous_usernames.last }, @me.access_token)
end

When(/^I search for users with (\w+)'s first name and last name$/) do |user_handle|
  user = known_user(user_handle)
  $zuul.admin_find_user({ first_name: user.first_name, last_name: user.last_name }, @me.access_token)
end

When(/^I search for users with (\w+)'s user id$/) do |user_handle|
  user = known_user(user_handle)
  $zuul.admin_find_user({ user_id: user.id }, @me.access_token)
end

When(/^I search for users with an unregistered (.+)$/) do |attributes|
  case attributes
  when "email address"
    $zuul.admin_find_user({ username: random_email }, @me.access_token)
  when "first and last name"
    $zuul.admin_find_user({ first_name: random_name, last_name: random_name }, @me.access_token)
  when "user id"
    $zuul.admin_find_user({ user_id: 999999999 }, @me.access_token)
  else
    pending "attributes '#{attributes}' are not supported by the test"
  end
end

When(/^I search for users with empty (.+)$/) do |attributes|
  case attributes
  when "email address"
    $zuul.admin_find_user({ username: "" }, @me.access_token)
  when "first and last name"
    $zuul.admin_find_user({ first_name: "", last_name: "" }, @me.access_token)
  when "user id"
    $zuul.admin_find_user({ user_id: "" }, @me.access_token)
  else
    pending "attributes '#{attributes}' are not supported by the test"
  end
end

When(/^I search for users with only a (first|last) name$/) do |name_position|
  search_term = name_position == "first" ? :first_name : :last_name
  $zuul.admin_find_user({ search_term => random_name }, @me.access_token)
end

Then(/^the response is a list containing (#{CAPTURE_INTEGER}) users?$/) do |count|
  expect(last_response.status).to eq(200)
  list = last_response_json
  expect(list["items"]).to be_a_kind_of(Array)
  expect(list["items"]).to have(count).users
end

Then(/^the (first|second) user matches (\w+)'s attributes$/) do |index, user_handle|
  expected_user = known_user(user_handle)
  actual_user = last_response_json["items"][index == "first" ? 0 : 1]
  expect(actual_user["user_username"]).to eq(expected_user.username)
  expect(actual_user["user_first_name"]).to eq(expected_user.first_name)
  expect(actual_user["user_last_name"]).to eq(expected_user.last_name)
  expect(actual_user["user_allow_marketing_communications"]).to eq(expected_user.allow_marketing_communications)
end

def register_random_user(handle, username: nil)
  user = TestUser.new.generate_details
  user.username = username if username
  user.register
  Cucumber::Rest::Status.ensure_status_class(:success)

  @known_users ||= {}
  @known_users[handle] = user

  user
end

def known_user(handle)
  if handle =~ /^(I|me)$/i
    user = @me
  else
    user = @known_users[handle]
  end
  raise "Test Error: Unknown user '#{user_handle}'" unless user
  user
end