When(/^I request user information for myself(, without my access token)?$/) do |no_token|
  access_token = @me.access_token unless no_token
  $zuul.get_user_info(@me.local_id, access_token)
end

When(/^I request user information for a nonexistent user$/) do
  nonexistent_user_id = @me.local_id.to_i + 1000
  $zuul.get_user_info(nonexistent_user_id, @me.access_token)
end

When(/^I request user information for the other user$/) do
  $zuul.get_user_info(@you.local_id, @me.access_token)
end

When(/^I change my email address$/) do
  @me.username = random_email
end

When(/^I change my ((?!.*client's ).+) to "(.+)"$/) do |name, value|
  method_name = "#{oauth_param_name(name)}="
  @me.send(method_name, value)
end

# Inverts a Boolean attribute of the user
When(/^I change whether I (.+)$/) do |name|
  get_method_name = oauth_param_name(name)
  set_method_name = "#{get_method_name}="
  new_value = !@me.send(get_method_name)
  @me.send(set_method_name, new_value)
end

When(/^I request my user information be updated(, without my access token)?$/) do |no_token|
  access_token = @me.access_token unless no_token
  $zuul.update_user(@me, access_token)
end

When(/^I request the other user's information be updated$/) do
  $zuul.update_user(@you, @me.access_token)
end

Then(/^(?:the response|it) contains (basic|complete) user information matching my(?: new)? details$/) do |format|
  validate_user_information_response(format.to_sym)
end