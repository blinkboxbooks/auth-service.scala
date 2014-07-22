Given(/^I have tried to authenticate with the wrong password (#{CAPTURE_INTEGER}) times within(?: the same)? (?:#{CAPTURE_INTEGER}) seconds$/) do |attempts|
  pending "Password throttling is not part of Zuul any more as it belongs to SSO now" unless TEST_CONFIG[:in_proc]
  use_username_and_password_credentials
  @credentials["password"] = random_password
  attempts.times { @me.authenticate(@credentials) }
end

Given(/^I have tried to change my password with the wrong password (#{CAPTURE_INTEGER}) times within(?: the same)? (?:#{CAPTURE_INTEGER}) seconds$/) do |attempts|
  pending "Password throttling is not part of Zuul any more as it belongs to SSO now" unless TEST_CONFIG[:in_proc]
  @password_params = { old_password: random_password, new_password: "sensibleNewPssw0rd" }
  attempts.times { $zuul.change_password(@password_params, @me.access_token) }
end

Given(/^I have tried to authenticate with an unregistered email address (#{CAPTURE_INTEGER}) times within (?:#{CAPTURE_INTEGER}) seconds$/) do |attempts|
  pending "Password throttling is not part of Zuul any more as it belongs to SSO now" unless TEST_CONFIG[:in_proc]
  @me = TestUser.new.generate_details
  use_username_and_password_credentials
  @credentials["password"] = random_password
  attempts.times { @me.authenticate(@credentials) }
end

When(/^I provide that email address and any password$/) do
  pending "Password throttling is not part of Zuul any more as it belongs to SSO now" unless TEST_CONFIG[:in_proc]
  use_username_and_password_credentials
end
