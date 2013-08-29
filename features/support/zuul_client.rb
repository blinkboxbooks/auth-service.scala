class ZuulClient
  include HTTParty

  def initialize(server_uri, proxy_uri = nil)
    self.class.base_uri server_uri.to_s
    self.class.http_proxy proxy_uri.host, proxy_uri.port if proxy_uri
  end

  def authenticate(params)
    http_post "/oauth2/token", params
  end

  def get_client_info(client_id, access_token)
    http_get "/clients/#{client_id}", {}, access_token
  end

  def get_clients_info(access_token)
    http_get "/clients", {}, access_token
  end

  def get_user_info(user_id, access_token)
    http_get "/users/#{user_id}", {}, access_token
  end

  def get_access_token(access_token)
    params = {access_token: access_token}
    http_get "/tokeninfo", params, access_token
  end

  def refresh_access_token(access_token)

  end

  def extend_elevated_session(access_token)
    params = {access_token: access_token}
    http_post "/tokeninfo", params, access_token
  end

  def register_client(client, access_token)
    params = { client_name: client.name, client_model: client.model }
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

  def revoke(refresh_token, access_token = nil)
    http_post "/tokens/revoke", { refresh_token: refresh_token }, access_token
  end

  def update_client(client, access_token)
    params = {}
    params[:name] = client.name if client.name_changed?
    params[:model] = client.model if client.model_changed?
    http_patch "/clients/#{client.local_id}", params, access_token
  end

  def update_user(user, access_token)
    params = {}
    params[:username] = user.username if user.username_changed?
    params[:password] = user.password if user.password_changed?
    params[:first_name] = user.first_name if user.first_name_changed?
    params[:last_name] = user.last_name if user.last_name_changed?
    params[:accepted_terms_and_conditions] = user.accepted_terms_and_conditions if user.accepted_terms_and_conditions_changed?
    params[:allow_marketing_communications] = user.allow_marketing_communications if user.allow_marketing_communications_changed?
    http_patch "/users/#{user.local_id}", params, access_token
  end

  private

  def http_get(uri, params={}, access_token = nil)
    headers = { "Accept" => "application/json" }
    headers["Authorization"] = "Bearer #{access_token}" if access_token
    self.class.get(uri.to_s, headers: headers, params: params)
    # File.open("last_response.html", "w") { |f| f.write(HttpCapture::RESPONSES.last.body) }
    HttpCapture::RESPONSES.last
  end

  def http_patch(uri, body_params, access_token = nil)
    http_send(:patch, uri, body_params, access_token)
  end

  def http_post(uri, body_params, access_token = nil)
    http_send(:post, uri, body_params, access_token)
  end

  def http_send(verb, uri, body_params, access_token = nil)    
    headers = { "Accept" => "application/json", "Content-Type" => "application/x-www-form-urlencoded" }
    headers["Authorization"] = "Bearer #{access_token}" if access_token
    body_params.reject! { |k, v| v.nil? }
    body_params = URI.encode_www_form(body_params) unless body_params.is_a?(String)
    self.class.send(verb, uri.to_s, headers: headers, body: body_params)  
    # File.open("last_response.html", "w") { |f| f.write(HttpCapture::RESPONSES.last.body) }
    HttpCapture::RESPONSES.last
  end

end