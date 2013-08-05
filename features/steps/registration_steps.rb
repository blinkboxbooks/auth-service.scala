
Given(/^I have registered an account$/) do
  @me = TestUser.new.register
end

Given(/^I have provided valid registration details$/) do
  @me = TestUser.new
end 

Given(/^I have not accepted the terms and conditions$/) do
  @me.accepted_terms_and_conditions = false
end

Given(/^I have not allowed marketing communications$/) do
  @me.allow_marketing_communications = false
end

Given(/^I have provided valid registration details, except (.+) which is missing$/) do |name|
  @me ||= TestUser.new  
  method_name = "#{oauth_param_name(name)}="
  @me.send(method_name, nil)
end

Given(/^I have provided valid registration details, except (.+) which is "(.*)"$/) do |name, value|
  @me ||= TestUser.new  
  method_name = "#{oauth_param_name(name)}="
  @me.send(method_name, value)
end

Given(/^my (.+) is "(.+)"$/) do |name, value|
  method_name = "#{oauth_param_name(name)}="
  @me.send(method_name, value)
end

When(/^I provide the same registration details I previously registered with$/, :noop)

When(/^I submit the registration request$/) do
  @me.register
end

Then(/^the reason is that the email address is already taken$/) do
  expect(last_response_json["error_reason"]).to eq("username_already_taken")
end