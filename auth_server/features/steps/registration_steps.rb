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

def submit_registration_request
  @registration_details.should_not be nil
  post_request("/oauth2/token", @registration_details)
end

Given(/^I have registered an account$/) do
  generate_registration_details
  submit_registration_request
  check_response_access_tokens
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

When(/^I provide the same registration details I previously registered with$/) do
  # nothing to do
end

When(/^I submit the registration request$/) do
  submit_registration_request
end