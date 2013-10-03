
Given(/^I have (?:subsequently )?requested my password is reset using my email address$/) do
  $zuul.reset_password(username: @me.username)
end

Given(/^I have got a password reset token$/) do
  step "I have requested my password is reset using my email address"
  step "I receive a password reset email"
  @password_reset_token = email_message_value("/e:sendEmail/e:templateVariables/e:templateVariable[e:key='resetToken']/e:value")
end

Given(/^I have reset my password$/) do
  step "I have got a password reset token"
  step "I provide my password reset token and a new password"
  step "I submit the password reset request"
  step "the request succeeds"
end

When(/^I request my password is reset using my email address$/) do
  $zuul.reset_password(username: @me.username)
end

When(/^I request my password is reset using an unregistered email address$/) do
  $zuul.reset_password(username: random_email)
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

Then(/^I receive a (.+) email$/) do |email_type|
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