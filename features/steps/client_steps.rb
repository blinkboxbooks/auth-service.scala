
Given(/^I have registered a client$/) do
  @my_client = TestClient.new.generate_details
  @me.register_client(@my_client)
  expect(last_response.status).to eq(200)
end

Given(/^another user has registered a client$/) do
  @your_client = TestClient.new.generate_details
  @you.register_client(@your_client)
  expect(last_response.status).to eq(200)
end

Given(/^I have registered (#{CAPTURE_INTEGER}) clients$/) do |count|
  count.times do
    @me.register_client(TestClient.new.generate_details)
  end
end

When(/^I provide a client name(?: of "(.*)")?$/) do |name|
  @my_client ||= TestClient.new.generate_details
  @my_client.name = name if name
end

When(/^I do not provide a client name$/) do
  @my_client ||= TestClient.new.generate_details
  @my_client.name = nil
end

When(/^I submit (?:a|the) client registration request(, without my access token)?$/) do |no_token|
  @my_client ||= TestClient.new.generate_details
  user = no_token ? TestUser::ANONYMOUS : @me
  user.register_client(@my_client)
end

When(/^I request client information for my client(, without my access token)?$/) do |no_token|
  access_token = @me.access_token unless no_token
  $zuul.get_client_info(@my_client.local_id, access_token)
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

Then(/^(?:the response|it) contains client information, (including a|excluding the) client secret$/) do |including|
  client_secret_expectation = including == "including a" ? :required : :prohibited
  verify_client_information_response(client_secret: client_secret_expectation)
end

Then(/^(?:the response|it) contains a list of (#{CAPTURE_INTEGER}) client's information(?:, excluding the client secret)?$/) do |count|
  client_list = last_response_json
  expect(client_list["clients"]).to be_instance_of(Array)
  # TODO: Need to verify the client information looks correct
end

Then(/^the client name should match the provided name$/) do
  expect(last_response_json["client_name"]).to eq(@my_client.name)
end

Then(/^a client name should have been created for me$/) do
  client_info = last_response_json
  expect(client_info["client_name"]).to_not be_nil
  expect(client_info["client_name"]).to_not be_empty
end

Then(/^the response indicates that the client credentials are incorrect$/) do
  expect(last_response.status).to eq(400)
  expect(last_response_json["error"]).to eq("invalid_client")
end

Then(/^the reason is that the client limit has been reached$/) do
  expect(last_response_json["error_reason"]).to eq("client_limit_reached")
end