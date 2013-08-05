class ZuulClient
  include HTTParty

  attr_accessor :access_token

  def initialize(server_uri)
    self.class.base_uri server_uri.to_s
  end

  def get_user_info(user_id)
    http_get "/users/#{user_id}"
  end

  def register_user(user)
    params = {
      grant_type: "urn:blinkbox:oauth:grant-type:registration",
      username: user.username,
      password: user.password,
      first_name: user.first_name,
      last_name: user.last_name,
      accepted_terms_and_conditions: user.accepted_terms_and_conditions,
      allow_marketing_communications: user.allow_marketing_communications
    }
    http_post "/oauth2/token", params.keep_if { |k, v| !v.nil? }              
  end

  private

  def http_get(uri)
    headers = { 
      "Accept" => "application/json"
    }
    headers["Authorization"] = "Bearer #{access_token}" if access_token
    headers.merge!(@request_headers) if defined?(@request_headers)

    begin
      self.class.get(uri.to_s, headers: headers)    
    rescue HTTParty::ResponseError
    end
    @response = HttpCapture::RESPONSES.last
    # File.open("last_response.html", "w") { |f| f.write(HttpCapture::RESPONSES.last.body) }
    @response
  end

  def http_post(uri, body)    
    headers = { 
      "Accept" => "application/json",
      "Content-Type" => "application/x-www-form-urlencoded" 
    }
    headers["Authorization"] = "Bearer #{access_token}" if access_token

    headers.merge!(@request_headers) if defined?(@request_headers)
    body = URI.encode_www_form(body) unless body.is_a?(String)
    begin
      self.class.post(uri.to_s, headers: headers, body: body)    
    rescue HTTParty::ResponseError
    end
    @response = HttpCapture::RESPONSES.last
    File.open("last_response.html", "w") { |f| f.write(HttpCapture::RESPONSES.last.body) }
    @response
  end

end