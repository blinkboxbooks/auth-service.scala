class SSOTestUser
  tracked_attr_accessor :username,
                        :password,
                        :first_name,
                        :last_name,
                        :accepted_terms_and_conditions,
                        :allow_marketing_communications

  attr_accessor :access_token,
                :refresh_token,
                :id

  def authenticate(credentials)
    response = $sso.authenticate(credentials)
    if response.status == 200
      token_info = ::JSON.parse(response.body)
      @access_token = token_info["access_token"]
      @refresh_token = token_info["refresh_token"] if token_info["refresh_token"]
    end
    response
  end
end
