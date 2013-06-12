
def submit_client_registration_request
  @client_info ||= {}
  post_json_request("/oauth2/client", @client_info)
end

def check_client_information_response
  @response.code.to_i.should == 200
  @client_response = MultiJson.load(@response.body)
  @client_response["client_id"].should_not be nil
  @client_response["client_secret"].should_not be nil
  @client_response["client_secret_expires_at"].should == 0
  @client_response["registration_access_token"].should_not be nil
  @client_response["registration_client_uri"].should_not be nil
end