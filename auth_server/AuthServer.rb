require "sinatra/base"
require "openssl"
require "json"
require "sandal"
require "active_record"
require "scrypt"
require "./environment.rb"

class User < ActiveRecord::Base
  has_many :refresh_tokens
  has_many :devices

  validates :first_name, length: { within: 1..50 }
  validates :last_name, length: { within: 1..50 }
  validates :email, format: { with: /.+@.+\..+/ }
  validates :password_hash, length: { within: 64..256 }
end

class RefreshToken < ActiveRecord::Base
  belongs_to :user
  belongs_to :device
  has_one :access_token, dependent: :destroy

  validates :token, length: { is: 32 }
end

class AccessToken < ActiveRecord::Base
  belongs_to :refresh_token
end

class Device < ActiveRecord::Base
  belongs_to :user
  has_one :refresh_token, dependent: :destroy

  validates :name, length: { within: 1..50 }
  validates :client_secret, length: { is: 32 }
  validates :client_access_token, length: { is: 32 }
end

# email = "greg@blinkbox.com"
# user = User.find_by_email(email)
# unless user
#   user = User.new(email: email, first_name: "Greg", last_name: "Beech")
#   user.save
# end

# if user.devices.count == 0
#   user.devices << Device.new do |device|
#     device.name = "Greg"s iPad"
#     device.client_secret = SecureRandom.random_bytes(32)
#     device.client_access_token = SecureRandom.random_bytes(32)
#   end
#   user.save
# end

# if user.refresh_tokens.count == 0
#   user.refresh_tokens << RefreshToken.new do |refresh_token|
#     refresh_token.token = SecureRandom.random_bytes(32)
#     refresh_token.expires_at = Time.now + (60 * 60 * 24 * 90)
#     refresh_token.access_token = AccessToken.new
#   end
#   user.save
# end

# p user.devices
# p user.refresh_tokens
# p user.refresh_tokens.first.created_at



class AuthServer < Sinatra::Base
  include Sandal::Util

  ACCESS_TOKEN_LIFETIME = 1800
  MIN_PASSWORD_LENGTH = 6

  get "/oauth2/token" do
    handle_token_request(params)
  end

  post "/oauth2/token" do
    handle_token_request(params)
  end

  post "/user" do
    first_name, last_name, email, password = params[:first_name], params[:last_name], params[:username], params[:password]
    if password.nil? || password.length < MIN_PASSWORD_LENGTH
      halt 400, JSON.generate({ "error" => "invalid_request", "error_description" => "password must be at least #{MIN_PASSWORD_LENGTH} characters" })
    end
    
    password_hash = SCrypt::Password.create(password)
    user = User.new(first_name: first_name, last_name: last_name, email: email, password_hash: password_hash) 
    begin
      user.save!
    rescue ActiveRecord::RecordInvalid => e
      halt 400, JSON.generate({ "error" => "invalid_request", "error_description" => e.message })
    end
    
    issue_tokens(user)
  end

  private

  def handle_token_request(params)
    grant_type = params[:grant_type]
    if grant_type.nil?
      halt 400, JSON.generate({ "error" => "invalid_request", "error_description" => "grant_type is required." })
    end

    case grant_type
    when "password"
      handle_password_flow(params)
    when "refresh_token"
      handle_refresh_token_flow(params)
    else
      halt 400, JSON.generate({ "error" => "unsupported_grant_type", "error_description" => "Use the 'password' grant_type." })
    end
  end

  def handle_password_flow(params)
    username = params[:username]
    password = params[:password]
    client_id = params[:client_id]

    if username.nil? || password.nil?
      halt 400, JSON.generate({ "error" => "invalid_request", "error_description" => "The username and password parameters are required." })
    end

    user = User.find_by_email(username)
    unless user && password == "1234$abcd" # hard-coded password for now
      halt 400, JSON.generate({ "error" => "invalid_grant", "error_description" => "The username and/or password is incorrect." })
    end

    if client_id
      device_id = client_id.match(/urn:blinkboxbooks:id:device:(\d+)/)[0] rescue nil
      client_secret = jwt_base64_decode(params[:client_secret]) rescue nil
      unless device_id && client_secret
        halt 400, JSON.generate({ "error" => "invalid_request", "error_description" => "The client_id and/or client_secret is incorrect." })
      end
      device = Device.find_by_id(device_id)
      unless device && device.client_secret == jwt_base64_decode(client_secret)
        halt 400, JSON.generate({ "error" => "invalid_request", "error_description" => "The client_id and/or client_secret is incorrect." })
      end
    end

    issue_tokens(user, device)
  end

  def handle_refresh_token_flow(params)
    token_value = params[:refresh_token]
    if token_value.nil?
      halt 400, JSON.generate({ "error" => "invalid_request", "error_description" => "The refresh_token parameter is required." })
    end
    refresh_token = RefreshToken.get_by_token(token_value)
    if refresh_token.nil?
      halt 400, JSON.generate({ "error" => "invalid_request", "error_description" => "The refresh_token parameter is invalid." })
    end

    # TODO: Check device etc.

    # TODO: Handle the flow
  end

  def issue_tokens(user, device = nil)
    refresh_token = RefreshToken.new do |token|
      token.user = user
      token.device = device
      token.token = SecureRandom.random_bytes(32)
      token.access_token = AccessToken.new
    end
    refresh_token.save

    JSON.generate({
      "access_token" => create_access_token(refresh_token),
      "token_type" => "bearer",
      "expires_in" => ACCESS_TOKEN_LIFETIME,
      "refresh_token" => jwt_base64_encode(SecureRandom.random_bytes(32))
    })
  end

  def create_access_token(refresh_token)
    issued_at = Time.now
    claims = JSON.generate({
      "sub" => "urn:blinkboxbooks:id:user:#{refresh_token.user.id}",
      "iat" => issued_at.to_i,
      "exp" => (issued_at + ACCESS_TOKEN_LIFETIME).to_i,
      "tid" => refresh_token.access_token.id.to_s,
      "bbb/uid" => refresh_token.user.id,
    })
    claims["bbb/cid"] = "urn:blinkboxbooks:id:device:#{refresh_token.device.id}" if refresh_token.device

    signer = Sandal::Sig::ES256.new(File.read("./keys/auth_server_ec_priv.pem"))
    jws_token = Sandal.encode_token(claims, signer, { "kid" => "/bbb/auth/sig/ec/1" })

    # TODO: Uncomment to use an RSA signed token instead of an ECDSA one
    # signer = Sandal::Sig::RS256.new(File.read("./keys/auth_server_rsa_priv.pem"))
    # jws_token = Sandal.encode_token(claims, signer, { kid: "/bbb/auth/sig/rsa/1" })

    encrypter = Sandal::Enc::A128GCM.new(Sandal::Enc::Alg::Direct.new(File.read("./keys/shared_aes128.key")))
    Sandal.encrypt_token(jws_token, encrypter, { "kid" => "/bbb/svcs/enc/a128/1", "cty" => "JWT" })
  end

end