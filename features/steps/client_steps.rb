
Given(/^I have registered a client$/, :register_new_client)

Given(/^I have registered (#{CAPTURE_INTEGER}) clients$/) do |count|
  (1..count).each { register_new_client }
end

Given(/^I have provided a client name(?: of "(.+)")?$/) do |name|
  name ||= "My Test Client"
  generate_client_registration_details(name)
end

Given(/^I have not provided a client name$/) do
  generate_client_registration_details(nil)
end

When(/^I submit (?:a|the) client registration request$/, :submit_client_registration_request)

When(/^I submit the client information request$/) do
  get_request(@client_response["client_uri"])
end

Then(/^the response contains client information, (including a|excluding the) client secret$/) do |including|
  client_secret_expectation = including == "including a" ? :required : :prohibited
  verify_client_information_response(client_secret: client_secret_expectation)
end

Then(/^the client name should match the provided name$/) do
  expect(@client_response["client_name"]).to eq(@client_registration_details["client_name"])
end

Then(/^a client name should have been created for me$/) do
  expect(@client_response["client_name"]).to_not be_nil
end

Then(/^the response indicates that the client credentials are incorrect$/) do
  expect(@response.status).to be_between(400, 401)
  expect(@response["WWW-Authenticate"]).to_not be_nil if @response.status == 401
  @response_json = MultiJson.load(@response.body)
  expect(@response_json["error"]).to eq("invalid_client")
end