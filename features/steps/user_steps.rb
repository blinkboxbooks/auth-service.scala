
When(/^I submit the user information request$/) do
  @user_response = get_request(@user_info["user_uri"])
end

When(/^I submit the user information request for a different user$/) do
  different_user_id = @user_info["user_id"][/\d+$/].to_i - 1
  @user_response = get_request("/users/#{different_user_id}")
end

Then(/^(?:the response|it) contains (basic|complete) user information matching the registration details$/) do |format|
  verify_user_information_response(format.to_sym)
end

