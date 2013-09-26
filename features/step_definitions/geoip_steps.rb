# note that these steps use the X-Forwarded-For header because it's the easiest way to spoof the
# IP address of a user to test geoIP. we need to support the X-Forwarded-For header as otherwise
# you end up just checking the geolocation of the last proxy the user went through, but this just
# shows how easy it is to programmatically spoof it. our apps won't do that, but it doesn't give
# us any protection from people who invoke the API through code to register surreptitiously‎.

Given(/^my IP address would geolocate me in ([A-Z]{2})$/) do |country_code|
  # these IP addresses are taken from the largest allocated block for each country as this is 
  # assumed to be the least likely to change. however, this step may need to be updated if
  # the IP addresses do change when the geoIP data file is updated.
  $zuul.headers["X-Forwarded-For"] = case country_code
                                     when "FR" then "57.23.2.19"
                                     when "GB" then "25.60.254.154"
                                     when "IE" then "87.39.20.103"
                                     when "US" then "3.49.104.149"
                                     else raise "Country code '#{country_code}' not supported."
                                     end
end

Given(/^my IP address is in the range (.+)$/) do |range|
  range = IPAddress.parse(range)
  $zuul.headers["X-Forwarded-For"] = range.address
end

Given(/^my IP address cannot be geolocated$/) do
  $zuul.headers["X-Forwarded-For"] = "1.0.0.0" # reserved non-private address
end

Then(/^the reason is that my country is geoblocked$/) do
  expect(last_response_json["error_reason"]).to eq("country_geoblocked")
end