
def generate_client_registration_details(name = "Test Client")
  @client_registration_details = {}
  @client_registration_details["client_name"] = name unless name.nil?
end

def submit_client_registration_request
  @client_registration_details ||= {}
  post_www_form_request("/clients", @client_registration_details)
end

def verify_client_information_response(client_secret = :required)
  expect(@response.status).to eq(200)
  @client_info = MultiJson.load(@response.body)
  expect(@client_info["client_id"]).to_not be_nil
  expect(@client_info["client_uri"]).to_not be_nil
  expect(@client_info["client_name"]).to_not be_nil
  expect(@client_info["client_secret"]).to_not be_nil if client_secret == :required
  expect(@client_info["client_secret"]).to be_nil if client_secret == :prohibited
end

def register_new_client
  add_access_token_request_header
  generate_client_registration_details
  submit_client_registration_request
  verify_client_information_response
end