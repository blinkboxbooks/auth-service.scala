
Given(/^I have provided my access token$/, :add_access_token_request_header)
Given(/^I have not provided my access token$/, :remove_access_token_request_header)
Given(/^I have provided an incorrect access token$/, :add_invalid_access_token_request_header)

Then(/^the response contains an access token and a refresh token$/, :validate_user_token_response)
Then(/^the response contains an access token$/) do
  validate_user_token_response(refresh_token: :optional)
end

Then(/^the request fails because it is invalid$/) do
  expect(@response.status).to eq(400)
  @response_json = MultiJson.load(@response.body)
  expect(@response_json["error"]).to eq("invalid_request")
end

Then(/^the request fails because I am unauthorised$/) do
  expect(@response.status).to eq(401)
end

Then(/^the request fails because this is forbidden$/) do
  expect(@response.status).to eq(403)
end
