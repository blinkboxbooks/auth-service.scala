
Then(/^the response contains an access token and a refresh token$/, :validate_user_token_response)
Then(/^the response contains an access token$/) do
  validate_user_token_response(refresh_token: :optional)
end

Then(/^the request succeeds$/) do
  Cucumber::Rest::Status.ensure_status_class(:success)
end

Then(/^the request fails because it is invalid$/) do
  expect(last_response.status).to eq(400)
  authenticate_header = Hash[*last_response['WWW-Authenticate'].scan(/([^\ ]+)="([^\"]+)"/).flatten]
  expect(authenticate_header["error"]).to eq("invalid_request")
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
