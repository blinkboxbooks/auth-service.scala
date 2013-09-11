Given(/^I have registered an account$/) do
  @me = TestUser.new.generate_details
  @me.register
  expect(last_response.status).to eq(200)
end

Given(/^another user has registered an account$/) do
  @you = TestUser.new.generate_details
  @you.register
  expect(last_response.status).to eq(200)
end

When(/^I provide valid registration details$/) do
  @me = TestUser.new.generate_details
end

When(/^I have( not)? accepted the terms and conditions$/) do |not_accepted|
  @me.accepted_terms_and_conditions = !not_accepted
end

When(/^I have( not)? allowed marketing communications$/) do |not_allowed|
  @me.allow_marketing_communications = !not_allowed
end

When(/^I provide valid registration details, except (.+) which is missing$/) do |name|
  @me ||= TestUser.new.generate_details
  method_name = "#{oauth_param_name(name)}="
  @me.send(method_name, nil)
end

When(/^I provide valid registration details, except (.+) which is "(.*)"$/) do |name, value|
  @me ||= TestUser.new.generate_details
  method_name = "#{oauth_param_name(name)}="
  @me.send(method_name, value)
end

When(/^my (.+) is "(.+)"$/) do |name, value|
  method_name = "#{oauth_param_name(name)}="
  @me.send(method_name, value)
end

When(/^I provide the same registration details I previously registered with$/, :noop)

When(/^I submit the registration request$/) do
  @me.register
end

Then(/^the reason is that the email address is already taken$/) do
  authenticate_header = Hash[*last_response['WWW-Authenticate'].scan(/([^\ ]+)="([^\"]+)"/).flatten]
  expect(authenticate_header["error_reason"]).to eq("username_already_taken")
end