Given(/^I obtain an access token using my email address and password$/) do
  use_username_and_password_credentials
  @me.authenticate(@credentials)
  expect(last_response.status).to eq(200)
end

Given(/^I request information about the access token$/) do
  $zuul.get_access_token(@me.access_token)
end

Then(/^it is (not elevated|elevated to critical)$/) do |elevation|
  elev = elevation.equal?('not elevated') ? :NONE : :CRITICAL
  validate_access_token_info_response(token_elevation = elev)
end

Then(/^the critical privileges expire (#{CAPTURE_INTEGER}) minutes from now$/) do |minutes|
  expect(last_response_json["token_elevation_expires_in"]).to be_within(10).of(minutes * 60)
end

Given(/^I wait for (?:over )?(#{CAPTURE_INTEGER}) minutes$/) do |minutes|
  sleep(minutes*60)
end

When(/^I request that my elevated session be extended$/) do
  $zuul.extend_elevated_session(@me.accees_token)
end

Then(/^the request fails because I am not authorised$/) do
  expect(last_response.status).to eq(401)
end

Then(/^the reason is that my identity is considered unverified$/) do
  expect(last_response_json["error_reason"]).to eq("unverified_identity")
end