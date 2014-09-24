class SSOTestUser
  tracked_attr_accessor :username,
                        :password,
                        :first_name,
                        :last_name,
                        :accepted_terms_and_conditions,
                        :allow_marketing_communications,
                        :scope

  attr_accessor :access_token,
                :refresh_token,
                :id

  def generate_details(user_scope)
    @first_name = random_name
    @last_name = random_name
    @username = random_email
    @password = random_password
    @accepted_terms_and_conditions = true
    @allow_marketing_communications = true
    @scope = 'sso:' + user_scope
    self
  end

  def register
    response = $sso.register_user(self)
    if response.status == 200
      token_info = ::JSON.parse(response.body)
      @access_token = token_info["access_token"]
      @refresh_token = token_info["refresh_token"]
    end
    response
  end

  def authenticate(credentials)
    response = $sso.authenticate(credentials)
    if response.status == 200
      token_info = ::JSON.parse(response.body)
      @access_token = token_info["access_token"]
      @refresh_token = token_info["refresh_token"] if token_info["refresh_token"]
    end
    response
  end

  def authenticate_books(credentials)
    response = $zuul.authenticate(credentials)
    if response.status == 200
      token_info = ::JSON.parse(response.body)
      @access_token = token_info["access_token"]
      @refresh_token = token_info["refresh_token"] if token_info["refresh_token"]
      @id = token_info["user_id"]
      @local_id = @id[/\d+$/]
      @password = credentials["password"] if credentials["password"] # handle password resets
    end
    response
  end
end
