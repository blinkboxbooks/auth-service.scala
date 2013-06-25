
def generate_user_registration_details
  @user_registration_details = {
    "grant_type" => "urn:blinkbox:oauth:grant-type:registration",
    "first_name" => "John",
    "last_name" => "Doe",
    "username" => random_email,
    "password" => random_password
  }
end

def submit_user_registration_request
  @user_registration_details.should_not be nil
  post_www_form_request("/oauth2/token", @user_registration_details)
end

def register_new_user
  generate_user_registration_details
  submit_user_registration_request
  check_response_tokens
end