
Given(/^I have (not )?provided my access token$/) do |no_token|
  @request_headers ||= {}
  if no_token
    @request_headers.delete("Authorization")
  elsif
    @request_headers["Authorization"] = "Bearer #{@oauth_response["access_token"]}"
  end
end

Given(/^I have provided an incorrect access token$/) do
  @request_headers ||= {}
  @request_headers["Authorization"] = "Bearer not.a.valid.access.token"
end

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

Then(/^the response indicates that I am unauthorised$/) do
  @response.code.to_i.should == 401
end