
CAPTURE_OAUTH_PARAM = Transform(/.+/) do |readable_name|
  param_name = readable_name.downcase
  param_name = "username" if ["email", "email address"].include?(readable_name)
  param_name.tr(" ", "_")
end

CAPTURE_URI = Transform(%r{(?:http|ftp)s?://.+}) do |uri|
  URI.parse(uri)
end