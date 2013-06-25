
CAPTURE_URI = Transform(%r{https?://.+}) do |uri|
  URI.parse(uri)
end