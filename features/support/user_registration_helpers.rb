
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
  expect(last_response.status).to eq(200)
  @user_info = MultiJson.load(last_response.body)
  expect(@user_info["access_token"]).to_not be_nil
  expect(@user_info["token_type"]).to match(/\Abearer\Z/i)
  expect(@user_info["expires_in"]).to be > 0
  expect(@user_info["refresh_token"]).to_not be_nil if refresh_token == :required

  # merge so that we keep the old refresh token if a new one wasn't issued
  (@user_tokens ||= {}).merge!(@user_info)
end

def verify_user_information_response(format = :complete, user = @me)
  expect(last_response.status).to eq(200)
  @user_info = MultiJson.load(last_response.body)
  expect(@user_info["user_id"]).to_not be_nil
  expect(@user_info["user_uri"]).to_not be_nil
  expect(@user_info["user_username"]).to eq(user.username)
  expect(@user_info["user_first_name"]).to eq(user.first_name)
  expect(@user_info["user_last_name"]).to eq(user.last_name)
  if format == :complete
    expect(@user_info["user_allow_marketing_communications"]).to eq(user.allow_marketing_communications)
  else
    expect(@user_info["user_allow_marketing_communications"]).to be_nil
  end
end

def register_new_user
  generate_user_registration_details
  submit_user_registration_request
  validate_user_token_response
end