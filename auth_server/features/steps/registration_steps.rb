
Given(/^I have registered an account$/) do
  generate_registration_details
  submit_registration_request
  check_response_tokens
end

Given(/^I have provided valid registration details$/) do
  generate_registration_details
end

Given(/^I have provided valid registration details, except (#{CAPTURE_OAUTH_PARAM}) which is missing$/) do |name|
  generate_registration_details
  @registration_details.delete(name)
end

Given(/^I have provided valid registration details, except (#{CAPTURE_OAUTH_PARAM}) which is "(.*)"$/) do |name, value|
  generate_registration_details
  @registration_details[name] = value
end

When(/^I provide the same registration details I previously registered with$/) do
  # nothing to do
end

When(/^I submit the registration request$/) do
  submit_registration_request
end