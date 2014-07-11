
# TODO: This file needs cleaning up and the step calls moved into methods
# I'm just getting the functionality in as quickly as I can for the moment

Given(/^I have (?:subsequently )?requested my password is reset using my email address$/) do
  $zuul.reset_password(username: @me.username)
end

Given(/^I have got a valid password reset token$/) do
  step "I have requested my password is reset using my email address"
  step "I receive a password reset email"
  @password_reset_token = email_message_value("/e:sendEmail/e:templateVariables/e:templateVariable[e:key='resetToken']/e:value")
end

Given(/^I have got an invalid password reset token$/) do
  @password_reset_token = random_password
end

Given(/^I have got two valid password reset tokens$/) do
  step "I have got a valid password reset token"

  step "I have requested my password is reset using my email address"
  step "I receive a password reset email"
  @password_reset_token_2 = email_message_value("/e:sendEmail/e:templateVariables/e:templateVariable[e:key='resetToken']/e:value")
end

Given(/^I have reset my password using (?:my|the first) password reset token$/) do
  step "I provide my password reset token and a new password"
  step "I submit the password reset request"
  step "the request succeeds"
end

Given(/^I have reset my password$/) do
  step "I have got a valid password reset token"
  step "I have reset my password using my password reset token"
end

Given(/^I subsequently authenticate using my email address and password$/, :obtain_access_and_token_via_username_and_password)

Given(/^(\w+) has got a valid password reset token$/) do |user_handle|
  user = known_user(user_handle)
  $zuul.reset_password(username: user.username)
  @email_message = Nokogiri::XML(Blinkbox::Zuul::Server::Email.sent_messages.pop)
  @password_reset_token = email_message_value("/e:sendEmail/e:templateVariables/e:templateVariable[e:key='resetToken']/e:value")
end

When(/^I request my password is reset using my email address$/) do
  $zuul.reset_password(username: @me.username)
end

When(/^I request my password is reset using an unregistered email address$/) do
  $zuul.reset_password(username: random_email)
end

When(/^I check whether my password reset token is valid$/) do
  $zuul.validate_password_reset_token(password_reset_token: @password_reset_token)
end

When(/^I provide my password reset token and a new password$/) do
  @old_password = @me.password
  use_password_reset_token_credentials
end

When(/^I provide my password reset token, a new password, and my client credentials$/) do
  @old_password = @me.password
  use_password_reset_token_credentials
  include_client_credentials
end

When(/^I provide my password reset token, but not a new password$/) do
  use_password_reset_token_credentials
  @credentials.delete("password")
end

When(/^I provide a new password, but not a password reset token$/) do
  use_password_reset_token_credentials
  @credentials.delete("password_reset_token")
end

When(/^I provide a new password, but an invalid password reset token$/) do
  use_password_reset_token_credentials
  @credentials["password_reset_token"] = random_password
end

When(/^the new password does not satisfy the password policy$/) do
  @credentials["password"] = "short"
end

When(/^I provide my email address and (new|old) password$/) do |password_version|
  use_username_and_password_credentials
  @credentials["password"] = @old_password if password_version == "old"
end

When(/^I provide the second password reset token and a new password$/) do
  use_password_reset_token_credentials(@password_reset_token_2)
end

When(/^(\w+) obtains an access token using (?:his|her|their) password reset token( and client credentials)?$/) do |user_handle, with_client|
  user = known_user(user_handle)
  use_password_reset_token_credentials(@password_reset_token)
  include_client_credentials(user.clients.last) if with_client
  user.authenticate(@credentials)
end

Then(/^I receive a (.+) email$/) do |email_type|
  pending "Messaging cannot be tested out-of-proc" unless TEST_CONFIG[:in_proc]

  @email_message = Nokogiri::XML(Blinkbox::Zuul::Server::Email.sent_messages.pop)

  email_message_value("/e:sendEmail/e:to/e:recipient/e:name") do |text| 
    expect(text).to eq("#{@me.first_name} #{@me.last_name}")
  end 
  email_message_value("/e:sendEmail/e:to/e:recipient/e:email") do |text| 
    expect(text).to eq(@me.username)
  end 
  email_message_value("/e:sendEmail/e:template") do |text| 
    expect(text).to eq(email_type.tr(" ", "_"))
  end
end

Then(/^(?:the email|it) contains a secure password reset link$/) do
  email_message_value("/e:sendEmail/e:templateVariables/e:templateVariable[e:key='resetLink']/e:value") do |text| 
    expect(text).to match(/https:\/\//) # the reset link should be a secure page
    expect(text).to match(URI::regexp)  # and should be a value uri
  end
end

Then(/^(?:the email|it) contains a password reset token with at least (#{CAPTURE_INTEGER}) characters$/) do |min_length|
  email_message_value("/e:sendEmail/e:templateVariables/e:templateVariable[e:key='resetToken']/e:value") do |text| 
    expect(text.length).to be >=(min_length)
  end
end

Then(/^no email is sent$/) do
  pending "Messaging cannot be tested out-of-proc" unless TEST_CONFIG[:in_proc]
  expect(Blinkbox::Zuul::Server::Email.sent_messages.count).to eq(0)
end

def email_message_value(xpath)
  elem = @email_message.at_xpath(xpath, e: "http://schemas.blinkbox.com/books/emails/sending/v1")
  expect(elem).to_not be_nil
  if block_given?
    yield elem.text 
  else
    elem.text
  end
end