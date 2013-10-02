@authentication @passwords @password_reset @wip
Feature: Resetting a user's password
  As a user
  I want to be able to reset my password
  So that I can still log in when I forget it

  Background:
    Given I have registered an account

  Scenario: Requesting a password reset using my email address sends a password reset email
    When I request my password is reset using my email address
    Then the request succeeds
    And I receive a password reset email
    And it contains a secure password reset link
    And it contains a password reset token with at least 32 characters

  Scenario: Requesting a password reset using my email address does not prevent password authentication
    Initiating a password reset should not stop a user from authenticating using their existing
    email address and password, as they may remember the credentials after initiating it and not 
    need to complete the reset process.

    Given I have requested my password is reset using my email address
    When I provide my email address and password
    And I submit the authentication request
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And it is not cacheable

  Scenario: Trying to request a password reset using an unregistered email address
    To prevent trivial checking of whether an email address is registered, we don't differentiate
    in the response between a mail being successfully sent and nothing happening.

    When I request my password is reset using an unregistered email address
    Then the request succeeds
    But no email is sent

  Scenario: Resetting a password using a password reset token
    The password token is a single-use bearer token which is as legitimate a way to sign in as
    any other grant type, so we treat resetting the password as a sign in action.

    Given I have got a password reset token
    When I provide my password reset token and a new password
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And it is not cacheable

  Scenario: Resetting a password using a password reset token, with client credentials
    Just because a user has forgotten their password doesn't mean their client has forgotten its
    credentials. If performing a reset from a registered client, it should still provide the
    client credentials it was previously given when performing the password reset to authenticate.

    Given I have got a password reset token
    When I provide my password reset token, a new password, and my client credentials
    Then the response contains an access token
    And it contains basic user information matching my details
    And it contains client information, excluding the client secret
    And it is not cacheable

  Scenario: Trying to reset a password using a password reset token, without providing a new password
    Given I have got a password reset token
    When I provide my password reset token
    But I do not provide a new password
    Then the request fails because it is invalid

  Scenario: Trying to reset a password using a password reset token, with an invalid new password
    Given I have got a password reset token
    When I provide my password reset token and a new password
    But the new password does not satisfy the password policy
    Then the request fails because it is invalid

  Scenario: Trying to reset a password using a password reset token that has already been used
    Password reset tokens are single-use, so once it has been used you can't use it again.

    Given I have got a password reset token
    And I have reset my password using my password reset token
    When I provide my password reset token and a new password
    Then the request fails because the password reset token is invalid

  Scenario: Trying to reset a password using a password reset token, after authenticating with a password
    If the user requests a password reset token but then remembers their password and authenticates
    using it then the password reset token should be revoked as it is clearly no longer needed.

    Given I have got a password reset token
    And I subsequently authenticate using my email address and password
    When I provide my password reset token and a new password
    Then the request fails because the password reset token is invalid

  Scenario: Using a password reset token after another token has been requested
    You can request multiple password reset tokens and they're all valid to use.

    Given I have got a password reset token
    And I have subsequently requested my password is reset using my email address
    When I provide my password reset token and a new password
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And it is not cacheable

  Scenario: Trying to use a password reset token after another token has been used
    If you request multiple password reset tokens and then use one of them, then all of the other
    ones are revoked and cannot be used.

    Given I have got two password reset tokens
    And I have reset my password using the first password reset token
    When I provide the second password reset token and a new password
    Then the request fails because the password reset token is invalid

  Scenario: Trying to use an expired password reset token
    Given I have got a password reset token
    When I wait for over 24 hours
    When I provide my password reset token and a new password
    Then the request fails because the password reset token is invalid