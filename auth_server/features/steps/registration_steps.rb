
Given(/^I have registered an account$/, :register_new_user)
Given(/^I have provided valid registration details$/, :generate_user_registration_details) 

Given(/^I have provided valid registration details, except (.+) which is missing$/) do |name|
  generate_user_registration_details
  @registration_details.delete(oauth_param_name(name))
end

Given(/^I have provided valid registration details, except (.+) which is "(.*)"$/) do |name, value|
  generate_user_registration_details
  @registration_details[oauth_param_name(name)] = value
end

When(/^I provide the same registration details I previously registered with$/, :noop)
When(/^I submit the registration request$/, :submit_user_registration_request)