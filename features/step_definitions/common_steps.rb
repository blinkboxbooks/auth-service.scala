Then(/^the response contains an access token and a refresh token$/, :validate_user_token_response)
Then(/^the response contains an access token$/) do
  validate_user_token_response(refresh_token: :optional)
end

Then(/^the request succeeds$/) do
  Cucumber::Rest::Status.ensure_status_class(:success)
end

Then(/^the request fails because it is invalid$/) do
  expect(last_response.status).to eq(400)
  @response_json = ::JSON.parse(last_response.body)
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

Then(/^the request fails because it has been throttled$/) do
  expect(last_response.status).to eq(429)
end

# NB. At the moment this only tests the WWW-Authenticate header. In the future we may also
# need to test the body for error information too.
Then(/^the response does not include any error information$/) do
  expect(www_auth_header.keys).to_not include('error')
  expect(www_auth_header.keys).to_not include('error_reason')
  expect(www_auth_header.keys).to_not include('error_description')
end

Then(/^the response includes only authentication scheme information$/) do
  # § 5.3 demands there is nothing after Bearer
  # https://tools.mobcastdev.com/confluence/display/PT/Authentication+and+Authorisation
  expect(last_response['WWW-Authenticate']).to eq('Bearer')
end

Then(/^the response includes only expired token information$/) do
  expect(www_auth_header['error']).to eq('invalid_token')
  expect(www_auth_header['error_description']).to eq('The access token is invalid')
end

Then(/^the response includes low elevation level information$/) do
  expect(www_auth_header['error']).to eq('invalid_token')
  expect(www_auth_header['error_reason']).to eq('unverified_identity')
  expect(www_auth_header['error_description']).to eq('You need to re-verify your identity')
end

Then(/^the response tells me I have to wait for between (#{CAPTURE_INTEGER}) and (#{CAPTURE_INTEGER}) seconds to retry$/) do |min_interval, max_interval|
  retry_after = last_response['Retry-After']
  expect(retry_after).to match(/\d+/)
  expect(retry_after.to_i).to be_between(min_interval, max_interval)
end