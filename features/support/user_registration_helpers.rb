
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
  @user_registration_details ||= {}
  @user_response = post_www_form_request("/oauth2/token", @user_registration_details)
end

def validate_user_token_response(refresh_token = :required)
  @user_response.code.to_i.should == 200
  token_info = MultiJson.load(@response.body)
  token_info["access_token"].should_not be nil
  token_info["token_type"].downcase.should == "bearer"
  token_info["expires_in"].to_i.should > 0
  token_info["refresh_token"].should_not be nil if refresh_token == :required

  # merge so that we keep the old refresh token if a new one wasn't issued
  (@user_tokens ||= {}).merge!(token_info)
end

def register_new_user
  generate_user_registration_details
  submit_user_registration_request
  validate_user_token_response
end