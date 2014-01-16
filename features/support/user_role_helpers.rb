def role_token(role_name)
  case role_name
  when /(blinkbox )?employee/i then "emp"
  when /it operations/i then "ops"
  when /customer services manager/i then "csm"
  when /customer services representative/i then "csr"
  when /content manager/i then "ctm"
  else pending "The role name '#{role_name}' does not have a token mapping"
  end
end