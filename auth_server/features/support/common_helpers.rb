
def random_email
  chars = [*("A".."Z"), *("a".."z"), *("0".."9")]
  "#{chars.sample(32).join}@example.org"
end

def random_password
  char_groups = ["A".."Z", "a".."z", "0".."9", "!@£$%^&*(){}[]:;'|<,>.?/+=".split(//)]
  char_groups.map { |chars| chars.to_a.sample(5) }.flatten.shuffle.join
end

def post_request(path, body)
  url = servers[:auth].clone
  url.path = path
  headers = { "Content-Type" => "application/x-www-form-urlencoded" }
  body = URI.encode_www_form(body)
  begin
    @response = @agent.request_with_entity(:post, url, body, headers)
    # p @response.body
  rescue Mechanize::ResponseCodeError => e
    @response = e.page
    # p e.page.body
  end
end

def check_response_tokens(refresh_token = :required)
  @response.code.to_i.should == 200
  @oauth_response = MultiJson.load(@response.body)
  @oauth_response["access_token"].should_not be nil
  @oauth_response["token_type"].downcase.should == "bearer"
  @oauth_response["expires_in"].to_i.should > 0
  if refresh_token == :required
    @oauth_response["refresh_token"].should_not be nil
  end
end