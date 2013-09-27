Given(/^my new password is too short$/) do
  @password_params[:new_password] = "short"
end

Given(/^my new password is the same as my current password$/) do
  @password_params[:new_password] = @password_params[:old_password]
end

Given(/^I provide a wrong password as my current password$/) do
  @password_params[:old_password] = "wrong_password"
  expect(@me.password).to_not be eq(@password_params[:old_password])
end

Given(/^I do not provide a new password$/) do
  @password_params[:new_password] = ""
end

When(/^I provide valid password change details$/) do
  @password_params = {old_password: @me.password, new_password: "sensibleNewPssw0rd"}
end

When(/^the request (does not include a|includes my desired) new password$/) do |include_password|
  case include_password
  when "does not include a"
    @password_params[:new_password] = nil
  when "includes my desired"
    @password_params[:new_password] = "newRandPassy"
  else
    raise ("The step definition syntax is incorrect, please check your step syntax and try again")
  end
end

When(/^I request my password be changed(, without my access token)?$/) do |without_access_token|
  access_token = @me.access_token rescue "failing_token"
  $zuul.change_password(@password_params, access_token)
end

Then(/^I am (not )?(?:still )?able to use my (new|old) password to authenticate$/) do |negative, password|
  @me.password = case password
                 when "old"
                   @password_params[:old_password]
                 when "new"
                   @password_params[:new_password]
                 else
                   raise("Do not know which password to use")
                 end
  step("I provide my email address and password")
  step("I submit the authentication request")
  verb = negative ? :to_not : :to
  expect(last_response.status).send(verb).eq(200)
end

Then(/^the reason is that the (old password is wrong|new password is too short|new password is missing)$/) do |reason|
  expect(last_response.status).to eq(400)
  @response_json = MultiJson.load(last_response.body)
  expect(@response_json["error"]).to eq("invalid_request")

  case reason
  when "old password is wrong"
    expect(@response_json["error_reason"]).to eq("invalid_old_password")
  when "new password is too short"
    expect(@response_json["error_reason"]).to eq("new_password_too_short")
  else
    raise("No error reason covered for #{reason}")
  end
end

