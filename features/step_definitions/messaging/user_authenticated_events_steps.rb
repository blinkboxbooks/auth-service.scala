Then(/^a user authenticated message is sent$/) do
  expect(Blinkbox::Zuul::Server::Reporting.sent_messages.size).to be >= 1
  @message = Nokogiri::XML(Blinkbox::Zuul::Server::Reporting.sent_messages.pop)
  reporting_message_value("users", "/e:authenticated")
end