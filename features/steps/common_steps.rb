
Given(/^I have (not )?provided my access token$/) do |no_token|
  @request_headers ||= {}
  if no_token
    @request_headers.delete("Authorization")
  else
    provide_access_token
  end
end

Given(/^I have provided an incorrect access token$/) do
  @request_headers ||= {}
  @request_headers["Authorization"] = "Bearer not.a.valid.access.token"
end

Then(/^the response contains an access token$/) do
  validate_user_token_response(refresh_token: :optional)
end

Then(/^the response contains an access token and a refresh token$/, :validate_user_token_response)

Then(/^the response indicates that the request was invalid$/) do
  @response.code.to_i.should == 400
  @response_json = MultiJson.load(@response.body)
  @response_json["error"].should == "invalid_request"
end

Then(/^the response indicates that I am unauthorised$/) do
  @response.code.to_i.should == 401
end

Then(/^the response indicates that this is forbidden$/) do
  @response.code.to_i.should == 403
end