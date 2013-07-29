
def generate_user_registration_details
  @user_registration_details = {
    "grant_type" => "urn:blinkbox:oauth:grant-type:registration",
    "first_name" => "John",
    "last_name" => "Doe",
    "username" => random_email,
    "password" => random_password,
    "accepted_terms_and_conditions" => true,
    "allow_marketing_communications" => true
  }
end

def submit_user_registration_request
  @user_registration_details ||= {}
  @user_response = post_www_form_request("/oauth2/token", @user_registration_details)
end

def validate_user_token_response(refresh_token = :required)
  expect(@user_response.status).to eq(200)
  token_info = MultiJson.load(@response.body)
  expect(token_info["access_token"]).to_not be_nil
  expect(token_info["token_type"]).to match(/\Abearer\Z/i)
  expect(token_info["expires_in"]).to be > 0
  expect(token_info["refresh_token"]).to_not be_nil if refresh_token == :required

  # merge so that we keep the old refresh token if a new one wasn't issued
  (@user_tokens ||= {}).merge!(token_info)
end

def register_new_user
  generate_user_registration_details
  submit_user_registration_request
  validate_user_token_response
end