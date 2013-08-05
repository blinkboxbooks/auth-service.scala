class TestUser

  attr_accessor :username,
                :password,
                :first_name, 
                :last_name, 
                :accepted_terms_and_conditions,
                :allow_marketing_communications

  attr_accessor :access_token,
                :refresh_token,
                :id,
                :local_id

  def initialize
    @first_name = "John"
    @last_name = "Doe"
    @username = random_email
    @password = random_password
    @accepted_terms_and_conditions = true
    @allow_marketing_communications = true
  end

  def get_info
    $zuul.get_user_info(@local_id)
  end

  def register
    response = $zuul.register_user(self)
    if response.status == 200
      token_info = MultiJson.load(response.body)
      @access_token = token_info["access_token"]
      @refresh_token = token_info["refresh_token"]
      @id = token_info["user_id"]
      @local_id = @id[/\d+$/]
    end
    self
  end

end