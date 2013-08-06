
def verify_client_information_response(client_secret = :required)
  expect(last_response.status).to eq(200)
  client_info = last_response_json
  expect(client_info["client_id"]).to_not be_nil
  expect(client_info["client_uri"]).to_not be_nil
  expect(client_info["client_name"]).to_not be_nil
  expect(client_info["client_secret"]).to_not be_nil if client_secret == :required
  expect(client_info["client_secret"]).to be_nil if client_secret == :prohibited
end