require "base64"
require "multi_json"
require "securerandom"

def random_email
  chars = [*("A".."Z"), *("a".."z"), *("0".."9")]
  "#{chars.sample(32).join}@example.org"
end

def random_password
  char_groups = ["A".."Z", "a".."z", "0".."9", "!@£$%^&*(){}[]:;'|<,>.?/+=".split(//)]
  char_groups.map { |chars| chars.to_a.sample(5) }.flatten.shuffle.join
end

Given(/^I have registered an account$/) do
  pending
end

Given(/^I have provided the details required to register$/) do
  @registration_details = {
    "first_name" => "John",
    "last_name" => "Doe",
    "username" => random_email,
    "password" => random_password
  }
end

Given(/^the (.+) is missing$/) do |detail|
  if defined?(@registration_details)
    @registration_details.delete(detail)
  end
end

Given(/^the (.+) has the value (.+)/) do |name, value|
  name = "username" if ["email", "email address"].include?(name)
  if defined?(@registration_details)
    @registration_details[name.to_sym] = value
  end
end

When(/^I submit the registration request$/) do
  url = servers["auth"].clone
  url.path = "/user"
  headers = { "Content-Type" => "application/x-www-form-urlencoded" }
  body = URI.encode_www_form(@registration_details)
  begin
    @response = @agent.request_with_entity(:post, url, body, headers)
  rescue Mechanize::ResponseCodeError => e
    @response = e.page
  end
end

Then(/^the response contains an access token and a refresh token$/) do
  oauth_response = MultiJson.load(@response.body)
  oauth_response["access_token"].should_not be nil
  oauth_response["refresh_token"].should_not be nil
end

Then(/^the response contains an error of type "(.+)"$/) do |error_code|
  oauth_response = MultiJson.load(@response.body)
  oauth_response["error"].should == error_code
end