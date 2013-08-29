Given(/^I obtain an access token using my email address and password$/) do
  obtain_access_and_token
end

Given(/^I request information about the access token$/) do
  $zuul.get_access_token_info(@me.access_token)
end

Then(/^it( is not elevated|s elevation is critical)$/) do |elevation|
  elev = elevation.equal?('not elevated') ? :NONE : :CRITICAL
  validate_access_token_info_response(token_elevation = elev)
end

Then(/^the critical privileges expire (#{CAPTURE_INTEGER}) minutes from now$/) do |num_of_minutes|
  expect(last_response_json["token_elevation_expires_in"]).to be_within(10.seconds).of(num_of_minutes.minutes)
end

Given(/^I wait for (?:over )?(#{CAPTURE_INTEGER}) minutes$/) do |num_of_minutes|
  sleep(num_of_minutes.minutes)
end

When(/^I request that my elevated session be extended$/) do
  $zuul.extend_elevated_session(@me.access_token)
end

Then(/^the request fails because I am unauthorised$/) do
  expect(last_response.status).to eq(401)
end

Then(/^the reason is that my identity is considered unverified$/) do
  expect(last_response_json["error_reason"]).to eq("unverified_identity")
end

Given(/^I have a non-privileged access token$/) do
  obtain_access_and_token
  sleep(10.minutes)
end