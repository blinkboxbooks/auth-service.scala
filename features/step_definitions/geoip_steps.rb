# To prevent external spoofing of source IP address using the X-Forwarded-For header, the server
# code only considers one IP address that preceeds an internal network address. This means that
# if we stay within the local network then we can use X-Forwarded-For to spoof the source for
# testing, but if we go outside (i.e. to any .com address) then that won't work and we need to
# use a real proxy. As a compromise, these tests use X-Forwarded-For when doing in-process testing
# to keep that nice and fast, but do it using a real GeoIP proxy for other tests.

Given(/^my IP address would geolocate me in ([A-Z]{2})$/) do |country_code|
  if TEST_CONFIG[:in_proc]
    # these IP addresses are taken from the largest allocated block for each country as this is 
    # assumed to be the least likely to change. however, this step may need to be updated if
    # the IP addresses do change when the geoIP data file is updated.
    source_ip = case country_code
                when "FR" then "57.23.2.19"
                when "GB" then "25.60.254.154"
                when "IE" then "87.39.20.103"
                when "US" then "3.49.104.149"
                else raise "Country code '#{country_code}' not supported."
                end
    $zuul.headers["X-Forwarded-For"] = source_ip
  else
    proxy_city = case country_code
                 when "FR" then "paris"
                 when "GB" then "london"
                 when "IE" then "newmarket"
                 when "US" then "washington"
                 else raise "Country code '#{country_code}' not supported."
                 end
    $zuul.use_proxy("https://monica:old636desk@#{proxy_city}.wonderproxy.com:80/")
  end
end

Given(/^my IP address is in the range (.+)$/) do |range|
  raise "Test Error: IP address range tests are only supported in-proc" unless TEST_CONFIG[:in_proc]
  range = IPAddress.parse(range)
  $zuul.headers["X-Forwarded-For"] = range.address
end

Given(/^my IP address cannot be geolocated$/) do
  raise "Test Error: Non-geolocatable tests are only supported in-proc" unless TEST_CONFIG[:in_proc]
  $zuul.headers["X-Forwarded-For"] = "1.0.0.0" # reserved non-private address
end

Then(/^the reason is that my country is geoblocked$/) do
  expect(last_response_json["error_reason"]).to eq("country_geoblocked")
end