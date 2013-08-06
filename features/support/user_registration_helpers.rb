
def validate_user_token_response(refresh_token = :required)
  expect(last_response.status).to eq(200)
  token_info = last_response_json
  expect(token_info["access_token"]).to_not be_nil
  expect(token_info["token_type"]).to match(/\Abearer\Z/i)
  expect(token_info["expires_in"]).to be > 0
  expect(token_info["refresh_token"]).to_not be_nil if refresh_token == :required
end