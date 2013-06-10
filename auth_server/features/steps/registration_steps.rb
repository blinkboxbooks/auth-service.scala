require "base64"
require "multi_json"
require "securerandom"
require_relative "step_helpers"

def generate_registration_details
  @registration_details = {
    "grant_type" => "urn:blinkboxbooks:oauth:grant-type:registration",
    "first_name" => "John",
    "last_name" => "Doe",
    "username" => random_email,
    "password" => random_password
  }
end

Given(/^I have provided valid registration details$/) do
  generate_registration_details
end

Given(/^I have provided valid registration details, except (.+) which is missing$/) do |name|
  generate_registration_details
  @registration_details.delete(oauth_param_name(name))
end

Given(/^I have provided valid registration details, except (.+) which is "(.*)"$/) do |name, value|
  generate_registration_details
  @registration_details[oauth_param_name(name)] = value
end

Given(/^the (.+) has the value "(.*)"/) do |name, value|
  name = oauth_param_name(name)
  (@registration_details ||= {})[name.to_sym] = value
end

def submit_registration_request
  post_request("/oauth2/token", @registration_details)
end

When(/^I provide the same registration details I previously registered with$/) do
  # nothing to do
end

When(/^I submit the registration request$/) do
  post_request("/oauth2/token", @registration_details)
end

def check_response_access_tokens
  @response.code.to_i.should == 200
  oauth_response = MultiJson.load(@response.body)
  oauth_response["access_token"].should_not be nil
  oauth_response["refresh_token"].should_not be nil
end

Then(/^the response contains an access token and a refresh token$/) do
  check_response_access_tokens
end

Given(/^I have registered an account$/) do
  generate_registration_details
  submit_registration_request
  check_response_access_tokens
end