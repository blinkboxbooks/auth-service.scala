require 'net/http'
require 'uri'
require 'json'

# obtain an oauth2 access token using the password grant type
token_uri = URI('http://127.0.0.1:9393/oauth2/token')
token_uri.query = URI.encode_www_form({
  grant_type: 'password',
  username: 'greg@blinkbox.com',
  password: '1234$abcd'
})
res = Net::HTTP.get_response(token_uri) # => String

case Integer(res.code)
when 200
  res_data = JSON.parse(res.body)
  access_token = res_data['access_token']
  expires_in = res_data['expires_in']
  refresh_token = res_data['refresh_token']
else
  throw StandardError.new("failed to get oauth token (code #{res.code}): #{res.body}")
end

segment_lengths = access_token.split('.').map { |s| s.length }
puts "access_token.length = #{access_token.length}, segment_lengths = #{segment_lengths}"
puts res.body

# use the access token to obtain a protected resource
resource_uri = URI.parse('http://127.0.0.1:9394/my')
req = Net::HTTP::Get.new(resource_uri.to_s)
req['Authorization'] = "Bearer #{access_token}"

res = Net::HTTP.start(resource_uri.hostname, resource_uri.port) do |http|
  http.request(req)
end

puts "HTTP #{res.code}"
puts res.body

# token_uri.query = URI.encode_www_form({
#   grant_type: 'refresh_token',
#   refresh_token: refresh_token
# })
# res = Net::HTTP.get_response(token_uri) # => String

# puts "HTTP #{res.code}"
# puts res.body