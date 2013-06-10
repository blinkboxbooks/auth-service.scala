
Then(/^the response contains an access token$/) do
  check_response_tokens(refresh_token: :optional)
end

Then(/^the response contains an access token and a refresh token$/) do
  check_response_tokens
end

Then(/^the response indicates that the request was invalid$/) do
  @response.code.to_i.should == 400
  oauth_response = MultiJson.load(@response.body)
  oauth_response["error"].should == "invalid_request"
end