When(/^I request admin user information for (\w+)$/) do |user_handle|
  user = known_user(user_handle)
  $zuul.admin_get_user_info(user.local_id, @me.access_token)
end

 When(/^I request admin user information for a nonexistent user$/) do
  nonexistent_user_id = 999999999
  $zuul.admin_get_user_info(nonexistent_user_id, @me.access_token)
end

Then(/^the response contains the following user information matching (\w+)'s attributes:$/) do |user_handle, table|
  expect(last_response.status).to eq(200)
  user = known_user(user_handle)
  table.raw.flatten.each do |attribute|
    expected = user.send(oauth_param_name(attribute))
    actual = last_response_json["user_#{oauth_param_name(attribute)}"]
    expect(actual).to eq(expected)
  end
end

Then(/^the the response includes (\w+)'s previous email addresses$/) do |user_handle|
  expect(last_response.status).to eq(200)
  user = known_user(user_handle)
  actual = last_response_json["user_previous_usernames"].map { |u| u["user_username"] }
  expect(actual).to eq(user.previous_usernames)
end