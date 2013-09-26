Given(/^I am (not )?authenticated$/) do |negative|
  step("I request that my refresh token be revoked") if negative
end

Given(/^the desired new password (passes|fails) validation$/) do |validity|
  case validity
  when /passes/
    expect(@password_params[:new_password].length).to be > 6
  when /fails/
    @password_params[:new_password] = "small"
  else
    raise("The new password strictly must either pass or fails validation.")
  end
end

Given(/^the desired new password is the same as my previous password$/) do
  @password_params[:new_password] = @me.password
end

Given(/^I create a request to change my password$/) do
  @password_params = { old_password: nil, new_password: nil }
end

Given(/^the request (contains my correct|does not contain my) existing password$/) do |correct_password|
  case correct_password
  when /contains my correct/
    @password_params[:old_password] = @me.password
  when /does not contain my/
    @password_params[:old_password] = "WrongRand0mPsswrd"
    expect(@password_params[:old_password]).to_not eql @me.password
  else
    raise ("The step definition syntax is incorrect, please check your step syntax and try again")
  end
end

When(/^the request (does not include a|includes my desired) new password$/) do |include_password|
  case include_password
  when /does not include a/
    @password_params[:new_password] = nil
  when /includes my desired/
    @password_params[:new_password] = "newRandPassy"
  else
    raise ("The step definition syntax is incorrect, please check your step syntax and try again")
  end
end

When(/^the request is submitted$/) do
  access_token = @me.access_token rescue "failing_token"
  $zuul.change_password(@password_params, )
end

Then(/^an error is returned$/) do
  expect(last_response.status).to_not eq(200)
end

Then(/^the password for the account (remains the same|is changed)$/) do |has_changed|
  current_password = case has_changed
                     when /remains the same/
                       @password_params[:old_password]
                     when /is changed/
                       @password_params[:new_password]
                     else
                       raise("The step definition syntax is incorrect, please check your step syntax and try again")
                     end

  @me.password = current_password
  step("I provide my email address and password")
  step("I submit the authentication request")
  expect(last_response.status).to eq(200)
end

Then(/^I am able to use my new password for all subsequent authentication attempts$/) do
  step("I provide my email address and password")
  step("I submit the authentication request")
  expect(last_response.status).to eq(200)
end