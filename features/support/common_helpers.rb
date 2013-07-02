
def noop; end

def random_email
  chars = [*("A".."Z"), *("a".."z"), *("0".."9")]
  "#{chars.sample(40).join}@blinkbox.com"
end

def random_password
  char_groups = ["A".."Z", "a".."z", "0".."9", "!@£$%^&*(){}[]:;'|<,>.?/+=".split(//)]
  char_groups.map { |chars| chars.to_a.sample(5) }.flatten.shuffle.join
end

def oauth_param_name(readable_name)
  param_name = readable_name.downcase
  param_name = "username" if ["email", "email address"].include?(readable_name)
  param_name.tr(" ", "_")
end