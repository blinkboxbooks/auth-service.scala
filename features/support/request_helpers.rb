
def add_access_token_request_header
  @request_headers ||= {}
  @request_headers["Authorization"] = "Bearer #{@user_tokens["access_token"]}"
end

def add_invalid_access_token_request_header
  @request_headers ||= {}
  @request_headers["Authorization"] = "Bearer not.a.valid.access.token"
end

def remove_access_token_request_header
  @request_headers ||= {}
  @request_headers.delete("Authorization")
end

def get_request(uri_or_path)
  uri = construct_auth_server_uri(uri_or_path)
  headers = { 
    "Accept" => "application/json"
  }
  headers.merge!(@request_headers) if defined?(@request_headers)
  begin
    @response = @agent.request_with_entity(:get, uri, "", headers)
    # p @response.body
  rescue Mechanize::ResponseCodeError => e
    @response = e.page
    # p e.page.body
  end
end

def post_www_form_request(uri_or_path, body)
  uri = construct_auth_server_uri(uri_or_path)
  headers = { 
    "Accept" => "application/json",
    "Content-Type" => "application/x-www-form-urlencoded" 
  }
  headers.merge!(@request_headers) if defined?(@request_headers)
  body = URI.encode_www_form(body) unless body.is_a?(String)
  begin
    # p body
    @response = @agent.request_with_entity(:post, uri, body, headers)
    # p @response.body
  rescue Mechanize::ResponseCodeError => e
    @response = e.page
    # p e.page.body
  end
  @response
end

def construct_auth_server_uri(uri_or_path)
  uri = URI.parse(uri_or_path)
  uri = URI.join(servers[:auth], uri) if uri.host.nil?
  uri
end