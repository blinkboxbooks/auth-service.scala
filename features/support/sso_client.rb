class SSOClient
  include HTTParty

  attr_accessor :headers
  SSO_BASIC_BOOKS_TOKEN = 'Ym9va3M6NWdRaEcxZG14djJtZkFZ'
  SSO_BASIC_MOVIES_TOKEN = 'bW92aWVzOkN6R0ZRVzdMVkN3NWxIeA=='
  SSO_BASIC_MUSIC_TOKEN = 'bXVzaWM6aFBnaHo2SW9KR3lqTlRF'
  ADMIN_PORT = ':444'

  def initialize(server_uri)
    self.class.base_uri(server_uri.to_s)
    self.class.debug_output($stderr) if TEST_CONFIG[:debug]
    @headers = {}
  end

  def authenticate(params)
    headers["Authorization"] = "Basic #{SSO_BASIC_BOOKS_TOKEN}"
    http_post "/v1/oauth2/token", params
  end

  def register_user(user)
    params = {
      grant_type: 'urn:blinkbox:oauth:grant-type:registration',
      username: user.username,
      password: user.password,
      first_name: user.first_name,
      last_name: user.last_name,
      accepted_terms_and_conditions: user.accepted_terms_and_conditions,
      allow_marketing_communications: user.allow_marketing_communications,
      scope: user.scope
    }
    case user.scope
      when 'sso:music'
        headers["Authorization"] = "Basic #{SSO_BASIC_MUSIC_TOKEN}"
      when 'sso:movies'
        headers["Authorization"] = "Basic #{SSO_BASIC_MOVIES_TOKEN}"
      else
        headers["Authorization"] = "Basic #{SSO_BASIC_BOOKS_TOKEN}"
    end
    http_post "/v1/oauth2/token", params
  end

  def link(access_token)
    params = {
      service_user_id: 'urn:blinkbox:zuul:user:00001',
      service_allow_marketing: true,
      service_tc_accepted_version: '1',
    }
    http_post "/v1/link", params, access_token
  end

  def admin_find_user(params = {}, access_token)
    self.class.base_uri << ADMIN_PORT
    http_get "/v1/admin/users", params, access_token
  end

  private

  def http_get(uri, params = {}, access_token = nil)
    http_call(:get, uri, params, access_token)
  end

  def http_call(verb, uri, params = {}, access_token = nil)
    headers = { "Accept" => "application/json" }.merge(@headers)
    headers["Authorization"] = "Bearer #{access_token}" if access_token
    self.class.send(verb, uri.to_s, headers: headers, query: params)
    self.class.base_uri(TEST_CONFIG[:sso_server].to_s)
    HttpCapture::RESPONSES.last
  end

  def http_post(uri, body_params, access_token = nil)
    http_send(:post, uri, body_params, access_token)
  end

  def http_send(verb, uri, body_params, access_token = nil)
    headers = { "Accept" => "application/json", "X-CSRF-Protection" => "engaged" }.merge(@headers)
    headers["Authorization"] = "Bearer #{access_token}" if access_token
    body_params.reject! { |_k, v| v.nil? }
    body_params = URI.encode_www_form(body_params) unless body_params.is_a?(String)
    self.class.send(verb, uri.to_s, headers: headers, body: body_params)
    self.class.base_uri(TEST_CONFIG[:sso_server].to_s)
    HttpCapture::RESPONSES.last
  end
end
