Given(/^I have revoked my refresh token$/) do
  $zuul.revoke(@me.refresh_token)
  expect(last_response.status).to eq(200)
end

When(/^I request that my refresh token be revoked(, without my access token)?$/) do |no_access_token|
  access_token = @me.access_token unless no_access_token
  $zuul.revoke(@me.refresh_token, access_token)
end

When(/^I request that a nonexistent refresh token be revoked$/) do
  $zuul.revoke("not_a_real_refresh_token")
end