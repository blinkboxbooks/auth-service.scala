
def generate_client_registration_details(name = "Test Client")
  @client_registration_details = {}
  @client_registration_details["client_name"] = name unless name.nil?
end

def submit_client_registration_request
  @client_registration_details ||= {}
  post_www_form_request("/clients", @client_registration_details)
end

def verify_client_information_response(client_secret = :required)
  @response.code.to_i.should == 200
  @client_response = MultiJson.load(@response.body)
  @client_response["client_id"].should_not be nil
  @client_response["client_uri"].should_not be nil
  @client_response["client_name"].should_not be nil
  @client_response["client_secret"].should_not be nil if client_secret == :required
  @client_response["client_secret"].should be nil if client_secret == :prohibited
end

def register_new_client
  provide_access_token
  submit_client_registration_request
  verify_client_information_response
end