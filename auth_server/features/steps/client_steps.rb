
Given(/^I have (not )?provided a client name$/) do |no_name|
  @client_info ||= {}
  @client_info["client_name"] = "My Test Client" unless no_name
end

Given(/^the client details I have provided are malformed$/) do
  @client_info = "this doesn't parse as json!"
end

When(/^I submit the client registration request$/) do
  @client_info ||= {}
  post_json_request("/oauth2/client", @client_info)
end

Then(/^the response contains client information, including a client secret$/) do
  @response.code.to_i.should == 200
  @client_response = MultiJson.load(@response.body)
  @client_response["client_id"].should_not be nil
  @client_response["client_secret"].should_not be nil
  @client_response["client_secret_expires_at"].should == 0
  @client_response["registration_access_token"].should_not be nil
  @client_response["registration_client_uri"].should_not be nil
end

Then(/^the client name should match the provided name$/) do
  @client_response["client_name"].should == @client_info["client_name"]
end

Then(/^a client name should have been created for me$/) do
  @client_response["client_name"].should_not be nil
end