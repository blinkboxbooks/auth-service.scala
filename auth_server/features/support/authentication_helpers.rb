
def provide_credentials(include_password = true)
  @credentials = {
    "grant_type" => "password",
    "username" => @registration_details["username"]
  }
  @credentials["password"] = @registration_details["password"] if include_password
end

def submit_authentication_request
  @credentials.should_not be nil
  post_www_form_request("/oauth2/token", @credentials)
end