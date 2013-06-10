
def oauth_param_name(readable_name)
  param_name = readable_name.downcase
  param_name = "username" if ["email", "email address"].include?(readable_name)
  param_name.tr(" ", "_")
end

def random_email
  chars = [*("A".."Z"), *("a".."z"), *("0".."9")]
  "#{chars.sample(32).join}@example.org"
end

def random_password
  char_groups = ["A".."Z", "a".."z", "0".."9", "!@£$%^&*(){}[]:;'|<,>.?/+=".split(//)]
  char_groups.map { |chars| chars.to_a.sample(5) }.flatten.shuffle.join
end

def post_request(path, body)
  url = servers["auth"].clone
  url.path = path
  headers = { "Content-Type" => "application/x-www-form-urlencoded" }
  body = URI.encode_www_form(body)
  begin
    @response = @agent.request_with_entity(:post, url, body, headers)
  rescue Mechanize::ResponseCodeError => e
    # p e.page.body
    @response = e.page
  end
end

def check_response_access_tokens
  @response.code.to_i.should == 200
  oauth_response = MultiJson.load(@response.body)
  oauth_response["access_token"].should_not be nil
  oauth_response["refresh_token"].should_not be nil
end