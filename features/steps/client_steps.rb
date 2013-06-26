
Given(/^I have registered a client$/, :register_new_client)

Given(/^I have (not )?provided a client name$/) do |no_name|
  @client_registration_details ||= {}
  if no_name
    @client_registration_details.delete("client_name")
  else
    @client_registration_details["client_name"] = "My Test Client" unless no_name
  end
end

When(/^I submit the client registration request$/, :submit_client_registration_request)

When(/^I submit the client information request$/) do
  begin
    @response = @agent.request_with_entity(:get, @client_response["client_uri"], "", @request_headers)
    # p @response.body
  rescue Mechanize::ResponseCodeError => e
    @response = e.page
    # p e.page.body
  end
end

Then(/^the response contains client information, (including a|excluding the) client secret$/) do |including|
  client_secret_expectation = including == "including a" ? :required : :prohibited
  check_client_information_response(client_secret: client_secret_expectation)
end

Then(/^the client name should match the provided name$/) do
  @client_response["client_name"].should == @client_registration_details["client_name"]
end

Then(/^a client name should have been created for me$/) do
  @client_response["client_name"].should_not be nil
end

Then(/^the response indicates that the client credentials are incorrect$/) do
  @response.code.to_i.should === 400..401
  @response["WWW-Authenticate"].should_not be nil if @response.code.to_i == 401
  @response_json = MultiJson.load(@response.body)
  @response_json["error"].should == "invalid_client"
end