CAPTURE_URI = Transform(%r{^https?://.+}) do |uri|
  URI.parse(uri)
end

CAPTURE_INTEGER = Transform(/^(?:-?\d+|no|one|two|three|four|five|six|seven|eight|nine|ten)$/) do |num|
  %w(no one two three four five six seven eight nine ten).index(num) || num.to_i
end