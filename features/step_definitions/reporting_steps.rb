When(/^they change their( client's)? (.+) to (.+)$/) do |client, detail, value|
  method_name = "#{oauth_param_name(detail)}="
  value = random_email if detail == "username"
  value = (value.downcase == "allow") if detail == "marketing communications preference"
  if client.nil?
    @old_me = @me.dup
    @me.send(method_name, value)
  else
    @my_old_client = @my_client.dup
    @my_client.send(method_name, value)
  end
end

Then(/^a (user|client) (registration|update|deregistration) message is sent$/) do |message_type, event_type|
  raise "Test Error: Users cannot be deregistered!" if message_type == "user" && event_type == "deregistration"
  expect(Blinkbox::Zuul::Server::Reporting.sent_messages).to have_at_least(1).message
  @message = Nokogiri::XML(Blinkbox::Zuul::Server::Reporting.sent_messages.pop)
  event = EVENT_TYPES[message_type]
  tag = event.chop
  case event_type
  when "registration"
    tag << "Created"
  when "update"
    tag << "Updated"
  when "deregistration"
    tag << "Deleted"
  end
  reporting_message_value(event, "/e:#{tag}")
end

Then(/^it contains the (user|client)'s id$/) do |message_type|
  case message_type
  when "user"
    tag = "userId"
    owner = @me
  when "client"
    tag = "deviceId"
    owner = @my_client
  end
  elem = @message.at_xpath("//xmlns:#{tag}")
  expect(elem).to_not be_nil
  expect(elem.text).to eq(owner.local_id)
end

Then(/^it contains the (user|client)'s details:$/) do |message_type, details|
  event = EVENT_TYPES[message_type]
  case message_type
  when "user"
    owner = @me
  when "client"
    owner = @my_client
  end
  details.rows.each do |r|
    validate_message_detail(event, r.first, owner)
  end
end

Then(/^it contains the (user|client)'s (old|new) details:$/) do |message_type, details_type, details|
  event = EVENT_TYPES[message_type]
  case message_type
  when "user"
    owner = details_type == "new" ? @me : @old_me
  when "client"
    owner = details_type == "new" ? @my_client : @my_old_client
  end
  details.rows.each do |r|
    validate_message_detail(event, "#{details_type} #{r.first}", owner)
  end
end

Then(/^it contains a (user|client) event timestamp$/) do |message_type|
  event = EVENT_TYPES[message_type]
  reporting_message_value(event, "//e:timestamp") do |text|
    expect(text).to eq(Time.parse(text).utc.iso8601)
  end
end

EVENT_TYPES = {
  "user" => "users",
  "client" => "devices"
}

def validate_message_detail(event, readable_detail, owner)
  path = readable_detail.split(":").map { |p| oauth_param_name(p.strip).camelize(:lower) }
  reporting_message_value(event, "//e:#{path.join("/e:")}") do |text|
    expect(text).to eq(owner.send(normalize_field(path.last)).to_s)
  end
end

def normalize_field(field)
  return "local_id" if field == "id"
  field.underscore
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
