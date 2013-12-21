When(/^they change their( client's)? (.+) to (.+)$/) do |client, detail, value|
  method_name = "#{oauth_param_name(detail)}="
  value = random_email if detail.include? "username"
  value = (value.downcase == "yes") if detail.include? "marketing"
  if client.nil?
    @me.send(method_name, value)
  else
    @my_client.send(method_name, value)
  end
end

Then(/^a (user (?:registration|update)|client (?:registration|update|deregistration)) message is sent$/) do |message_type|
  expect(Blinkbox::Zuul::Server::Reporting.sent_messages.size).to eq(1)

  @message = Nokogiri::XML(Blinkbox::Zuul::Server::Reporting.sent_messages.pop)
  event = message_type.include? "user" ? "users" : "devices"
  tag = event.chop
  case message_type.split.last
  when "registration"
    tag << "Created"
  when "update"
    tag << "Updated"
  when "deregistration"
    tag << "Deleted"
  end
  puts "event: #{event} tag: #{tag}"
  reporting_message_value(event, "/e:#{tag}")
end

Then(/^it contains the (user|client)'s details:$/) do |owner, details|
  #p details.rows
end

Then(/^it contains the user's (old|new) details$/) do |details_type|
  fields = %w(username firstName lastName allowMarketingCommunications)
  validate_message_details("users", "User", details_type, @me, @old_me, fields)
end

Then(/^it contains the client's (old|new|deregistration) details$/) do |details_type|
  case details_type
  when "new", "old"
    fields = %w(name brand model os)
    validate_message_type_details("devices", "Device", details_type, @my_client, @my_old_client, fields)
  when "deregistration"
    fields = %w(id name brand model os)
    validate_message_details("devices", "device", @my_old_client, fields)
  end
end

def validate_message_type_details(event, tag, type, new_owner, old_owner, fields)
  owner = type == "new" ? new_owner : old_owner
  validate_message_details(event, "#{type}#{tag}", owner, fields)
end

def validate_message_details(event, tag, owner, fields)
  fields.each do |field|
    reporting_message_value(event, "//e:#{tag}/e:#{field}") do |text|
      expect(text).to eq(owner.send(field.underscore))
    end
  end
end

def reporting_message_value(event, xpath)
  elem = @message.at_xpath(xpath, e: "http://schemas.blinkboxbooks.com/events/#{event}/v1")
  expect(elem).to_not be_nil
  if block_given?
    yield elem.text
  else
    elem.text
  end
end
