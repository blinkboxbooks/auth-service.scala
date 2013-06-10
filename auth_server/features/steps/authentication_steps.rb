require_relative "step_helpers"

Given(/^I have provided my email address$/) do
  @credentials = {
    "grant_type" => "password",
    "username" => @registration_details["username"]
  }
end

Given(/^I have provided my email address and password$/) do
  @credentials = {
    "grant_type" => "password",
    "username" => @registration_details["username"],
    "password" => @registration_details["password"]
  }
end

Given(/^the email address is different from the one I used to register$/) do
  @credentials["username"] = random_email    
end

Given(/^the password is (missing|incorrect)$/) do |error_type|
  case error_type
  when "missing" then @credentials.delete("password")
  when "incorrect" then @credentials["password"] = random_password
  end
end

When(/^I submit the authentication request$/) do
  post_request("/oauth2/token", @credentials)
end

Then(/^the response indicates that my credentials are incorrect$/) do
  @response.code.to_i.should == 400
  oauth_response = MultiJson.load(@response.body)
  oauth_response["error"].should == "invalid_grant"
end
