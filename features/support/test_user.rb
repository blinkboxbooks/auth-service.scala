require_relative "change_tracking"

class TestUser

  ANONYMOUS = TestUser.new

  tracked_attr_accessor :username,
                        :password,
                        :first_name,
                        :last_name,
                        :accepted_terms_and_conditions,
                        :allow_marketing_communications

  attr_accessor :access_token,
                :refresh_token,
                :id,
                :local_id,
                :clients,
                :previous_usernames

  def initialize
    @clients = []
    @previous_usernames = []
  end

  def after_username_changed(old_value, new_value)
    @previous_usernames.unshift(old_value) # most recent first
  end

  def generate_details
    @first_name = random_name
    @last_name = random_name
    @username = random_email
    @password = random_password
    @accepted_terms_and_conditions = true
    @allow_marketing_communications = true
    self
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
    response
  end

  def register_with_client(client)
    response = $zuul.register_user_with_client(self, client)
    if response.status == 200
      token_info = MultiJson.load(response.body)
      @access_token = token_info["access_token"]
      @refresh_token = token_info["refresh_token"]
      @id = token_info["user_id"]
      @local_id = @id[/\d+$/]
      @clients << response
    end
  end

  def authenticate(credentials)
    response = $zuul.authenticate(credentials)
    if response.status == 200
      token_info = MultiJson.load(response.body)
      @access_token = token_info["access_token"]
      @refresh_token = token_info["refresh_token"] if token_info["refresh_token"]
      @id = token_info["user_id"]
      @local_id = @id[/\d+$/]
      @password = credentials["password"] if credentials["password"] # handle password resets
    end
    response
  end

  def register_client(client)
    response = client.register(@access_token)
    @clients << client if response.status == 200
    response
  end

end