def validate_client_information_response(client_secret = :required)
  expect(last_response.status).to eq(200), "Expected status code 200. Got #{last_response.status}: #{www_auth_header['error']} (#{www_auth_header['error_description']})"
  client_info = last_response_json
  expect(client_info["client_id"]).to_not be_nil
  expect(client_info["client_uri"]).to_not be_nil
  expect(client_info["client_name"]).to_not be_nil
  expect(client_info["client_model"]).to_not be_nil
  expect(client_info["client_secret"]).to_not be_nil if client_secret == :required
  expect(client_info["client_secret"]).to be_nil if client_secret == :prohibited
  expect(client_info["last_used_date"]).to_not be_nil
end

def validate_user_information_response(format)
  expect(last_response.status).to eq(200)
  user_info = last_response_json
  expect(user_info["user_id"]).to_not be_nil
  expect(user_info["user_uri"]).to_not be_nil
  expect(user_info["user_username"]).to eq(@me.username)
  expect(user_info["user_first_name"]).to eq(@me.first_name)
  expect(user_info["user_last_name"]).to eq(@me.last_name)
  if format == :complete
    expect(user_info["user_allow_marketing_communications"]).to eq(@me.allow_marketing_communications)
  else
    expect(user_info["user_allow_marketing_communications"]).to be_nil
  end
end

def validate_user_token_response(refresh_token = :required)
  expect(last_response.status).to eq(200)
  token_info = last_response_json
  expect(token_info["access_token"]).to_not be_nil
  expect(token_info["token_type"]).to match(/\Abearer\Z/i)
  expect(token_info["expires_in"]).to be > 0
  expect(token_info["refresh_token"]).to_not be_nil if refresh_token == :required
end

def validate_access_token_info_response
  expect(last_response.status).to eq(200)
  token_info = last_response_json
  expect(token_info["token_status"]).to match(Regexp.new(%w{VALID INVALID}.join('|')))

  if (token_info["token_status"] == 'VALID')
    expect(token_info["token_elevation"]).to match(Regexp.new(%w{NONE ELEVATED CRITICAL}.join('|')))
    if token_info["token_elevation"] == 'NONE'
      expect(token_info["token_elevation_expires_in"]).to be_nil
    else
      expect(token_info["token_elevation_expires_in"]).to be_a_kind_of(Numeric)
    end
  else
    expect(token_info["token_elevation"]).to be_nil
    expect(token_info["token_elevation_expires_in"]).to be_nil
  end
end