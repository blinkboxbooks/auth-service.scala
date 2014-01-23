def noop;
end

def random_email
  chars = [*"a".."z", *"0".."9"]
  "#{chars.sample(40).join}@bbbtest.com"
end

def random_password
  chars = [*"A".."Z", *"a".."z", *"0".."9", *"!@£$%^&*(){}[]:;'|<,>.?/+=".split(//)]
  chars.sample(40).join
end

def random_name
  chars = [*"a".."z"]
  "#{[*"A".."Z"].sample(1).first}#{[*"a".."z"].sample(39).join}"
end

def oauth_param_name(readable_name)
  param_name = readable_name.downcase
  param_name.gsub!(/email( addresse)?s/, "usernames")
  param_name.gsub!(/email( address)?/, "username")
  param_name.tr(" ", "_")
end

def www_auth_header
  Hash[*last_response['WWW-Authenticate'].scan(/([^\ ]+)="([^\"]+)"/).flatten]
rescue
  {}
end