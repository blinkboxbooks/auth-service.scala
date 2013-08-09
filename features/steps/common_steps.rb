
Then(/^the response contains an access token and a refresh token$/, :validate_user_token_response)
Then(/^the response contains an access token$/) do
  validate_user_token_response(refresh_token: :optional)
end

Then(/^the request fails because it is invalid$/) do
  expect(last_response.status).to eq(400)
  @response_json = MultiJson.load(last_response.body)
  expect(@response_json["error"]).to eq("invalid_request")
end

Then(/^the request fails because I am unauthorised$/) do
  expect(last_response.status).to eq(401)
end

Then(/^the request fails because this is forbidden$/) do
  expect(last_response.status).to eq(403)
end

Then(/^the request fails because (?:.+) was not found$/) do
  expect(last_response.status).to eq(404)
end
