
def noop; end

def random_email
  chars = [*("A".."Z"), *("a".."z"), *("0".."9")]
  "#{chars.sample(32).join}@example.org"
end

def random_password
  char_groups = ["A".."Z", "a".."z", "0".."9", "!@£$%^&*(){}[]:;'|<,>.?/+=".split(//)]
  char_groups.map { |chars| chars.to_a.sample(5) }.flatten.shuffle.join
end

def oauth_param_name(readable_name)
  param_name = readable_name.downcase
  param_name = "username" if ["email", "email address"].include?(readable_name)
  param_name.tr(" ", "_")
end

def provide_access_token
  @request_headers ||= {}
  @request_headers["Authorization"] = "Bearer #{@oauth_response["access_token"]}"
end

def post_www_form_request(path, body, additional_headers = {})
  url = servers[:auth].clone
  url.path = path
  headers = { "Content-Type" => "application/x-www-form-urlencoded" }
  headers.merge!(@request_headers) if defined?(@request_headers)
  body = URI.encode_www_form(body) unless body.is_a?(String)
  begin
    # p body
    @response = @agent.request_with_entity(:post, url, body, headers)
    # p @response.body
  rescue Mechanize::ResponseCodeError => e
    @response = e.page
    # p e.page.body
  end
end

def post_json_request(path, body)
  url = servers[:auth].clone
  url.path = path
  headers = { "Content-Type" => "application/json" }
  headers.merge!(@request_headers) if defined?(@request_headers)
  body = MultiJson.dump(body) unless body.is_a?(String)
  begin
    # p body
    @response = @agent.request_with_entity(:post, url, body, headers)
    # p @response.body
  rescue Mechanize::ResponseCodeError => e
    @response = e.page
    # p e.page.body
  end
end

def check_response_tokens(refresh_token = :required)
  @response.code.to_i.should == 200
  body = MultiJson.load(@response.body)
  body["access_token"].should_not be nil
  body["token_type"].downcase.should == "bearer"
  body["expires_in"].to_i.should > 0
  if refresh_token == :required
    body["refresh_token"].should_not be nil
  end

  # merge here so that we keep the old refresh token if a new one wasn't issued
  if @oauth_response
    @oauth_response.merge!(body)
  else
    @oauth_response = body
  end
end