
When(/^I submit a user information request for myself$/) do
  @me.get_info
end

When(/^I submit a user information request for a nonexistent user$/) do
  nonexistent_user_id = @me.local_id.to_i + 100
  $zuul.get_user_info(nonexistent_user_id)
end

When(/^I submit a user information request for a different user$/) do
  @you ||= TestUser.new.register
  @you.get_info
end

Then(/^(?:the response|it) contains (basic|complete) user information matching my details$/) do |format|
  expect(last_response.status).to eq(200)
  info = last_response_json
  expect(info["user_id"]).to_not be_nil
  expect(info["user_uri"]).to_not be_nil
  expect(info["user_username"]).to eq(@me.username)
  expect(info["user_first_name"]).to eq(@me.first_name)
  expect(info["user_last_name"]).to eq(@me.last_name)
  if format == "complete"
    expect(info["user_allow_marketing_communications"]).to eq(@me.allow_marketing_communications)
  else
    expect(info["user_allow_marketing_communications"]).to be_nil
  end
end

