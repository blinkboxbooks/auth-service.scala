
When(/^I request my password is reset using my email address$/) do
  $zuul.reset_password(username: @me.username)
end

Then(/^I receive a (.+) email$/) do |email_type|
  @email_message = Nokogiri::XML(Blinkbox::Zuul::Server::Email.sent_messages.pop)

  validate_email_message("/e:sendEmail/e:to/e:recipient/e:name") do |text| 
    expect(text).to eq("#{@me.first_name} #{@me.last_name}")
  end 
  validate_email_message("/e:sendEmail/e:to/e:recipient/e:email") do |text| 
    expect(text).to eq(@me.username)
  end 
  validate_email_message("/e:sendEmail/e:template") do |text| 
    expect(text).to eq(email_type.tr(" ", "_"))
  end
end

Then(/^(?:the email|it) contains a secure password reset link$/) do
  validate_email_message("/e:sendEmail/e:templateVariables/e:templateVariable[e:key='resetLink']/e:value") do |text| 
    expect(text).to match(/https:\/\//) # the reset link should be a secure page
    expect(text).to match(URI::regexp)  # and should be a value uri
  end
end

Then(/^(?:the email|it) contains a password reset token with at least (#{CAPTURE_INTEGER}) characters$/) do |min_length|
  validate_email_message("/e:sendEmail/e:templateVariables/e:templateVariable[e:key='resetToken']/e:value") do |text| 
    expect(text.length).to be >=(min_length)
  end
end

def validate_email_message(xpath)
  elem = @email_message.at_xpath(xpath, e: "http://schemas.blinkbox.com/books/emails/sending/v1")
  expect(elem).to_not be_nil
  yield elem.text if block_given?
end