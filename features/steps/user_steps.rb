
When(/^I submit a user information request for the current user$/) do
  @user_response = get_request(@user_info["user_uri"])
end

When(/^I submit a user information request for a nonexistent user$/) do
  nonexistent_user_id = @user_info["user_id"][/\d+$/].to_i + 100
  @user_response = get_request("/users/#{nonexistent_user_id}")
end

When(/^I submit a user information request for a different user$/) do
  different_user_id = @user_info["user_id"][/\d+$/].to_i - 1
  @user_response = get_request("/users/#{different_user_id}")
end

Then(/^(?:the response|it) contains (basic|complete) user information matching the registration details$/) do |format|
  verify_user_information_response(format.to_sym)
end

