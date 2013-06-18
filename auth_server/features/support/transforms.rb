
CAPTURE_URI = Transform(%r{(?:http|ftp)s?://.+}) do |uri|
  URI.parse(uri)
end