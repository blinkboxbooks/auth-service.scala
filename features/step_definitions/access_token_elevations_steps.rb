Given(/^I obtain an access token using my email address and password$/) do
  obtain_access_and_token_via_username_and_password
end

Given(/^I have an? (critically |non-)?elevated access token$/) do |elevation_level|
  obtain_access_and_token_via_username_and_password
  case elevation_level
  when "critically "
    # Don't sleep
  when "non-"
    sleep(1.day)
    obtain_access_and_token_via_refresh_token
  else
    sleep(10.minutes)
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
  time_delta = 5
  delta_measurement = case time_unit
                        when /days?/
                          "minutes"
                        when /minutes/
                          "seconds"
                        else
                          raise "undefined unit of time for #{time_unit}"
                      end
  time_period = num.send(time_unit)
  time_period = time_period - negate.send(negate_unit) if negate
  expect(last_response_json["token_elevation_expires_in"]).to be_within(time_delta.send(delta_measurement)).of(time_period)
end
