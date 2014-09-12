Then(/^my details are correct in the SSO system$/) do
  @admin = SSOTestUser.new
  @admin.username = 'admin@blinkbox.com'
  @admin.password = 'Bl1nkb0x'
  obtain_access_and_token_via_username_and_password(@admin)
  $sso.admin_find_user({ username: @me.username }, @admin.access_token)
  validate_sso_user_response
end
