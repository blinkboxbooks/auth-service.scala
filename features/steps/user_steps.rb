
When(/^I request user information for myself(, without my access token)?$/) do |no_token|
  access_token = @me.access_token unless no_token
  $zuul.get_user_info(@me.local_id, access_token)
end

When(/^I request user information for a nonexistent user$/) do
  nonexistent_user_id = @me.local_id.to_i + 1000
  $zuul.get_user_info(nonexistent_user_id, @me.access_token)
end

When(/^I request user information for the other user$/) do
  $zuul.get_user_info(@you.local_id, @me.access_token)
end

Then(/^(?:the response|it) contains (basic|complete) user information matching my details$/) do |format|
  validate_user_information_response(format.to_sym)
end

