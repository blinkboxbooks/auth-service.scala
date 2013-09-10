
Then(/^the response contains an access token and a refresh token$/, :validate_user_token_response)
Then(/^the response contains an access token$/) do
  validate_user_token_response(refresh_token: :optional)
end

Then(/^the request succeeds$/) do
  Cucumber::Rest::Status.ensure_status_class(:success)
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

# NB. At the moment this only tests the WWW-Authenticate header. In the future we may also
# need to test the body for error information too.
Then(/^the response does not include any error information$/) do
  www_auth = Hash[*last_response['WWW-Authenticate'].scan(/([^\ ]+)="([^\"]+)"/).flatten]
  expect(www_auth.keys).to_not include('error')
  expect(www_auth.keys).to_not include('error_reason')
  expect(www_auth.keys).to_not include('error_description')
end