
Given(/^I have registered an account$/, :register_new_user)
Given(/^I have provided valid registration details$/, :generate_user_registration_details) 

Given(/^I have not accepted the terms and conditions$/) do
  generate_user_registration_details
  @user_registration_details["accepted_terms_and_conditions"] = false
end

Given(/^I have provided valid registration details, except (.+) which is missing$/) do |name|
  generate_user_registration_details
  @user_registration_details.delete(oauth_param_name(name))
end

Given(/^I have provided valid registration details, except (.+) which is "(.*)"$/) do |name, value|
  generate_user_registration_details
  @user_registration_details[oauth_param_name(name)] = value
end

Given(/^my (.+) is "(.+)"$/) do |name, value|
  @user_registration_details[oauth_param_name(name)] = value
end

When(/^I provide the same registration details I previously registered with$/, :noop)
When(/^I submit the registration request$/, :submit_user_registration_request)

Then(/^the reason is that the email address is already taken$/) do
  @response_json["error_reason"].should == "username_already_taken"
end