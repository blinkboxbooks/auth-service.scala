Given(/^(?:I have|a user has) registered a client$/) do
  @my_client = TestClient.new.generate_details
  @me.register_client(@my_client)
  expect(last_response.status).to eq(200)
end

Given(/^I have registered another client$/) do
  @my_other_client = TestClient.new.generate_details
  @me.register_client(@my_other_client)
  expect(last_response.status).to eq(200)
end

Given(/^another user has registered a client$/) do
  @your_client = TestClient.new.generate_details
  @you.register_client(@your_client)
  expect(last_response.status).to eq(200)
end

Given(/^I have registered (#{CAPTURE_INTEGER}) clients in total$/) do |count|
  count -= @me.clients.count
  count.times do
    @me.register_client(TestClient.new.generate_details)
  end
end

When(/^I provide the client registration details:$/) do |table|
  @my_client ||= TestClient.new.generate_details
  table.rows_hash.each { |k, v| @my_client.send("#{oauth_param_name(k)}=", v) }
end

When(/^I provide a client (.+?)(?: of "(.*)")?$/) do |name, value|
  @my_client ||= TestClient.new.generate_details
  @my_client.send("#{oauth_param_name(name)}=", value) if value
end

When(/^I provide a valid client$/) do
  @my_client ||= TestClient.new.generate_details
end

When(/^I do not provide a client (.+?)$/) do |name|
  @my_client ||= TestClient.new.generate_details
  @my_client.send("#{oauth_param_name(name)}=", nil)
end

When(/^(?:I|they) submit (?:a|the) client registration request(, without my access token)?$/) do |no_token|
  @my_client ||= TestClient.new.generate_details
  user = no_token ? TestUser::ANONYMOUS : @me
  user.register_client(@my_client)
end

When(/^I request client information for my( other)? client(, without my access token)?$/) do |other_client, no_token|
  client = other_client ? @my_other_client : @my_client
  access_token = @me.access_token unless no_token
  $zuul.get_client_info(client.local_id, access_token)
end

When(/^I request client information for my client, using an access token that is not bound to it$/) do
  step "I provide my email address and password"
  step "I submit the authentication request"
  step "I request client information for my client"
end

When(/^I request client information for a nonexistent client$/) do
  nonexistent_client_id = @my_client.local_id.to_i + 1000
  $zuul.get_client_info(nonexistent_client_id, @me.access_token)
end

When(/^I request client information for the other user's client$/) do
  $zuul.get_client_info(@your_client.local_id, @me.access_token)
end

When(/^I request client information for all my clients(, without my access token)?$/) do |no_token|
  access_token = @me.access_token unless no_token
  $zuul.get_clients_info(access_token)
end

When(/^I change my( other)? client's details to:$/) do |other_client, table|
  client = other_client ? @my_other_client : @my_client
  table.rows_hash.each { |k, v| client.send("#{oauth_param_name(k)}=", v) }
end

When(/^I do not change my client's details$/, :noop)

When(/^(?:I request my|they request their)( other)? client's information be updated(, without my access token)?$/) do |other_client, no_token|
  client = other_client ? @my_other_client : @my_client
  access_token = @me.access_token unless no_token
  $zuul.update_client(client, access_token)
end

When(/^I request the other user's client's information be updated$/) do
  $zuul.update_client(@your_client, @me.access_token)
end

Then(/^(?:the response|it) contains client information, (including a|excluding the) client secret$/) do |including|
  client_secret_expectation = including == "including a" ? :required : :prohibited
  validate_client_information_response(client_secret_expectation)
end

Then(/^(?:the response|it) contains a list of (#{CAPTURE_INTEGER}) client's information(?:, excluding the client secret)?$/) do |count|
  client_list = last_response_json
  expect(client_list["clients"]).to be_instance_of(Array)
  # TODO: Need to verify the client information looks correct
end

Then(/^the( other)? client details match the provided details$/) do |other_client|
  client = other_client ? @my_other_client : @my_client
  %w{name brand model os}.each do |detail|
    expect(last_response_json["client_#{detail}"]).to eq(client.send(detail))
  end
end

Then(/^the client (.+) is "(.+)"$/) do |name, value|
  expect(last_response_json["client_#{name.downcase}"]).to eq(value)
end

Then(/^its last used date is (#{CAPTURE_INTEGER}) days ago$/) do |num_days|
  required_date = (Time.now.utc - num_days.days).strftime("%Y-%m-%d")
  expect(last_response_json["last_used_date"]).to eq(required_date)
end

Then(/^the response indicates that the client credentials are incorrect$/) do
  expect(last_response.status).to eq(400)
  expect(last_response_json["error"]).to eq("invalid_client")
end

Then(/^the reason is that the client limit has been reached$/) do
  expect(last_response_json["error_reason"]).to eq("client_limit_reached")
end

Then(/^each client has a last used date$/) do
  client_list = last_response_json
  client_list["clients"].each do |client|
    expect(client["last_used_date"]).to match(/^\d\d\d\d-\d\d-\d\d$/)
  end
end
