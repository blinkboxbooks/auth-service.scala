
Given(/^I have provided my access token$/, :add_access_token_request_header)
Given(/^I have not provided my access token$/, :remove_access_token_request_header)
Given(/^I have provided an incorrect access token$/, :add_invalid_access_token_request_header)

Then(/^the response contains an access token and a refresh token$/, :validate_user_token_response)
Then(/^the response contains an access token$/) do
  validate_user_token_response(refresh_token: :optional)
end

Then(/^the response indicates that the request was invalid$/) do
  @response.status.should == 400
  @response_json = MultiJson.load(@response.body)
  @response_json["error"].should == "invalid_request"
end

Then(/^the response indicates that I am unauthorised$/) do
  @response.status.should == 401
end

Then(/^the response indicates that this is forbidden$/) do
  @response.status.should == 403
end