Given(/^(\w+) obtains? an access token using (?:my|his|her|their) email address and password$/) do |user_handle|
  obtain_access_and_token_via_username_and_password(known_user(user_handle))
end

Given(/^(\w+) obtains? an access token using (?:my|his|her|their) email address, password and client credentials$/) do |user_handle|
  user = known_user(user_handle)
  obtain_access_and_token_via_username_and_password(user, user.clients.last)
end

Given(/^(\w+) obtains? an access token using (?:my|his|her|their) refresh token$/) do |user_handle|
  obtain_access_and_token_via_username_and_password(known_user(user_handle))
end

Given(/^(\w+) obtains? an access token using (?:my|his|her|their) refresh token and client credentials$/) do |user_handle|
  user = known_user(user_handle)
  obtain_access_and_token_via_refresh_token(user, user.clients.last)
end

Given(/^I have an? (critically |non-)?elevated access token$/) do |elevation_level|
  obtain_access_and_token_via_username_and_password
  case elevation_level
  when "critically "
    sleep(10.seconds)
  when "non-"
    sleep(ELEVATION_CONFIG[:elevated_timespan] + 10.seconds)
    obtain_access_and_token_via_refresh_token
  else
    sleep(ELEVATION_CONFIG[:critical_timespan] + 10.seconds)
    obtain_access_and_token_via_refresh_token
  end
end

Given(/^I wait for (over )?(#{CAPTURE_INTEGER}) (seconds|minutes|hours|days?)$/) do |over, duration, time_unit|
  sleep_time = duration.send(time_unit)
  sleep_time = sleep_time + 2.seconds if over
  sleep(sleep_time)
end

When(/^I request information about the access token$/) do
  $zuul.get_access_token_info(@me.access_token)
end

When(/^I request information about my session, without my access token$/) do
  $zuul.get_access_token_info(nil)
end

When(/^I request information about my session, with an empty access token$/) do
  $zuul.get_access_token_info("")
end

When(/^I request information about my session, with an expired access token$/) do
  obtain_access_and_token_via_username_and_password
  step("I wait for 30 minutes")
  $zuul.get_access_token_info(@me.access_token)
end

Then(/^the response contains access token information$/) do
  validate_access_token_info_response
end

Then(/^it( is not elevated|s elevation is critical)$/) do |elevation|
  elev = elevation.include?('not elevated') ? 'NONE' : 'CRITICAL'
  expect(last_response_json['token_elevation']).to eql(elev)
end

When(/^it is elevated$/) do
  expect(last_response_json['token_elevation']).to eql('ELEVATED')
end

When(/^I request that my elevated session be extended$/) do
  $zuul.extend_elevated_session(@me.access_token)
end

Then(/^the reason is that my identity is unverified$/) do
  expect(www_auth_header["error_reason"]).to eq("unverified_identity")
end

When(/^the elevation expires (#{CAPTURE_INTEGER}) (minutes|days?) from now(?: minus (#{CAPTURE_INTEGER}) (minutes|days?))?$/) do |num, time_unit, negate, negate_unit|
  time_period = num.send(time_unit)
  time_period = time_period - negate.send(negate_unit) if negate

  ensure_elevation_expires_in(time_period)
end

When(/^the (critical )?elevation got extended$/) do |elevation|
  elev = elevation.include?('critical') ? 'CRITICAL' : 'ELEVATED'
  time_period = elev == 'CRITICAL' ? ELEVATION_CONFIG[:critical_timespan] : ELEVATION_CONFIG[:elevated_timespan]

  $zuul.get_access_token_info(@me.access_token)
  expect(last_response_json["token_elevation"]).to eql(elev)
  ensure_elevation_expires_in(time_period)
end
