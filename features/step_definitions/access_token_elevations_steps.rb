Given(/^I obtain an access token using my email address and password$/) do
  obtain_access_and_token
end

Given(/^I have a non-elevated access token$/) do
  obtain_access_and_token
  sleep(10.send(TIME_MEASUREMENT) + 1.second)
end

Given(/^I wait for (over )?(#{CAPTURE_INTEGER}) (minutes|seconds)$/) do |over, num_of_minutes, time_unit|
  sleep_time = num_of_minutes.send(TIME_MEASUREMENT)
  sleep_time = sleep_time + 2.seconds if over
  sleep(sleep_time)
end

When(/^I request information about the access token$/) do
  $zuul.get_access_token_info(@me.access_token)
end

Then(/^the response contains access token information$/) do
  validate_access_token_info_response
end

Then(/^it( is not elevated|s elevation is critical)$/) do |elevation|
  elev = elevation.equal?('not elevated') ? 'NONE' : 'CRITICAL'
  expect(last_response_json['token_elevation']).to eql(elev)
end

Then(/^the critical elevation expires (#{CAPTURE_INTEGER}) minutes from now$/) do |num_of_minutes|
  expect(last_response_json["token_elevation_expires_in"]).to be_within(10.seconds).of(num_of_minutes.send(TIME_MEASUREMENT))
end

When(/^I request that my elevated session be extended$/) do
  $zuul.extend_elevated_session(@me.access_token)
end

Then(/^the reason is that my identity is considered unverified$/) do
  authenticate_header = Hash[*last_response['WWW-Authenticate'].scan(/([^\ ]+)="([^\"]+)"/).flatten]
  expect(authenticate_header["error_reason"]).to eq("unverified_identity")
end