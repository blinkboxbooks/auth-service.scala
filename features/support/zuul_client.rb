class ZuulClient
  include HTTParty

  def initialize(server_uri)
    self.class.base_uri server_uri.to_s
  end

  def authenticate(params)
    http_post "/oauth2/token", params
  end

  def get_client_info(client_id, access_token)
    http_get "/clients/#{client_id}", access_token
  end

  def get_clients_info(access_token)
    http_get "/clients", access_token
  end

  def get_user_info(user_id, access_token)
    http_get "/users/#{user_id}", access_token
  end

  def register_client(client, access_token)
    params = { client_name: client.name }
    http_post "/clients", params, access_token
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
    http_post "/oauth2/token", params
  end

  private

  def http_get(uri, access_token = nil)
    headers = { "Accept" => "application/json" }
    headers["Authorization"] = "Bearer #{access_token}" if access_token
    self.class.get(uri.to_s, headers: headers)
    # File.open("last_response.html", "w") { |f| f.write(HttpCapture::RESPONSES.last.body) }
    HttpCapture::RESPONSES.last
  end

  def http_post(uri, body_params, access_token = nil)    
    headers = { "Accept" => "application/json", "Content-Type" => "application/x-www-form-urlencoded" }
    headers["Authorization"] = "Bearer #{access_token}" if access_token
    body_params.reject! { |k, v| v.nil? }
    body_params = URI.encode_www_form(body_params) unless body_params.is_a?(String)
    self.class.post(uri.to_s, headers: headers, body: body_params)  
    # File.open("last_response.html", "w") { |f| f.write(HttpCapture::RESPONSES.last.body) }
    HttpCapture::RESPONSES.last
  end

end