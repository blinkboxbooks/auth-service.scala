
def provide_user_credentials
  @credentials ||= {}
  @credentials["grant_type"] = "password"
  @credentials["username"] = @registration_details["username"]
  @credentials["password"] = @registration_details["password"]
end

def provide_client_credentials
  @credentials ||= {}
  @credentials["client_id"] = @client_response["client_id"]
  @credentials["client_secret"] = @client_response["client_secret"]
end

def submit_authentication_request
  @credentials.should_not be nil
  post_www_form_request("/oauth2/token", @credentials)
end