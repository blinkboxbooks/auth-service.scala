
When(/^I submit a user information request for myself(, without my access token)?$/) do |no_token|
  access_token = @me.access_token unless no_token
  $zuul.get_user_info(@me.local_id, access_token)
end

When(/^I submit a user information request for a nonexistent user$/) do
  nonexistent_user_id = @me.local_id.to_i + 100
  $zuul.get_user_info(nonexistent_user_id, @me.access_token)
end

When(/^I submit a user information request for a different user$/) do
  other_user = TestUser.new.generate_details
  other_user.register
  $zuul.get_user_info(other_user.local_id, @me.access_token)
end

Then(/^(?:the response|it) contains (basic|complete) user information matching my details$/) do |format|
  expect(last_response.status).to eq(200)
  user_info = last_response_json
  expect(user_info["user_id"]).to_not be_nil
  expect(user_info["user_uri"]).to_not be_nil
  expect(user_info["user_username"]).to eq(@me.username)
  expect(user_info["user_first_name"]).to eq(@me.first_name)
  expect(user_info["user_last_name"]).to eq(@me.last_name)
  if format == "complete"
    expect(user_info["user_allow_marketing_communications"]).to eq(@me.allow_marketing_communications)
  else
    expect(user_info["user_allow_marketing_communications"]).to be_nil
  end
end

