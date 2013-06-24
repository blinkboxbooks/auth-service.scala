
def generate_user_registration_details
  @registration_details = {
    "grant_type" => "urn:blinkboxbooks:oauth:grant-type:registration",
    "first_name" => "John",
    "last_name" => "Doe",
    "username" => random_email,
    "password" => random_password
  }
end

def submit_user_registration_request
  @registration_details.should_not be nil
  post_www_form_request("/oauth2/token", @registration_details)
end

def register_new_user
  generate_user_registration_details
  submit_user_registration_request
  check_response_tokens
end