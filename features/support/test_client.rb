require_relative "change_tracking"

class TestClient

  tracked_attr_accessor :name,
                        :brand,
                        :model,
                        :os

  attr_accessor :id,
                :local_id,
                :secret

  def generate_details
    @name = "My Test Client"
    @model = "Test Device"
    @os = "Test OS"
    @brand = "Test brand"
    self
  end

  def register(access_token)
    response = $zuul.register_client(self, access_token)
    if response.status == 200
      token_info = ::JSON.parse(response.body)
      @name = token_info["client_name"] unless @name
      @brand = token_info["client_brand"] unless @brand
      @model = token_info["client_model"] unless @model
      @os = token_info["client_os"] unless @os
      @id = token_info["client_id"]
      @local_id = @id[/\d+$/]
      @secret = token_info["client_secret"]
    end
    response
  end

end