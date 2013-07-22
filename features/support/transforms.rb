
CAPTURE_URI = Transform(%r{^https?://.+}) do |uri|
  URI.parse(uri)
end

CAPTURE_INTEGER = Transform(/^\d+/) do |num|
  num.to_i
end