@authentication @passwords @password_reset
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

  Scenario: Checking whether a valid password reset token is valid
    To allow clients to show a message to a user that the password reset token isn't valid before
    prompting them to enter their password, they need a method to check its validity.

    Given I have got a valid password reset token
    When I check whether my password reset token is valid
    Then the request succeeds

  Scenario: Checking whether an invalid password reset token is valid
    Given I have got an invalid password reset token
    When I check whether my password reset token is valid
    Then the request fails because it is invalid

  Scenario: Resetting a password using a password reset token
    The password token is a single-use bearer token which is as legitimate a way to sign in as
    any other grant type, so we treat resetting the password as a sign in action.

    Given I have got a valid password reset token
    When I provide my password reset token and a new password
    And I submit the password reset request
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And it is not cacheable

  Scenario: Resetting a password using a password reset token, with client credentials
    Just because a user has forgotten their password doesn't mean their client has forgotten its
    credentials. If performing a reset from a registered client, it should still provide the
    client credentials it was previously given when performing the password reset to authenticate.

    Given I have registered a client
    And I have got a valid password reset token
    When I provide my password reset token, a new password, and my client credentials
    And I submit the password reset request
    Then the response contains an access token
    And it contains basic user information matching my details
    And it contains client information, excluding the client secret
    And it is not cacheable

  Scenario: Trying to reset a password without a password reset token
    When I provide a new password, but not a password reset token
    And I submit the password reset request
    Then the request fails because it is invalid

  Scenario: Trying to reset a password with an invalid password reset token
    When I provide a new password, but an invalid password reset token
    And I submit the password reset request
    Then the response indicates that my password reset token is invalid

  Scenario: Trying to reset a password using a password reset token, without providing a new password
    Given I have got a valid password reset token
    When I provide my password reset token, but not a new password
    And I submit the password reset request
    Then the request fails because it is invalid

  Scenario: Trying to reset a password using a password reset token, with an invalid new password
    Given I have got a valid password reset token
    When I provide my password reset token and a new password
    But the new password does not satisfy the password policy
    And I submit the password reset request
    Then the request fails because it is invalid

  Scenario: Logging in with the new password after changing it
    Given I have reset my password
    When I provide my email address and new password
    And I submit the authentication request
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And it is not cacheable

  Scenario: Trying to log in with the old password after changing it
    Given I have reset my password
    When I provide my email address and old password
    And I submit the authentication request
    Then the response indicates that my credentials are incorrect

  Scenario: Trying to reset a password using a password reset token that has already been used
    Password reset tokens are single-use, so once it has been used you can't use it again.

    Given I have got a valid password reset token
    And I have reset my password using my password reset token
    When I provide my password reset token and a new password
    And I submit the password reset request
    Then the response indicates that my password reset token is invalid

  Scenario: Trying to use a password reset token after another token has been used
    If you request multiple password reset tokens and then use one of them, then all of the other
    ones are revoked and cannot be used.

    Given I have got two valid password reset tokens
    And I have reset my password using the first password reset token
    When I provide the second password reset token and a new password
    And I submit the password reset request
    Then the response indicates that my password reset token is invalid

  Scenario: Using a password reset token after another token has been requested
    You can request multiple password reset tokens and they're all valid to use. There's no real
    security risk as they'll all go to the same account, and this caters for the scenario where they
    may go to junk mail, then the user notices after requesting a couple of them and clicks on
    an arbitrary link which may not be the last one that was issued.

    Given I have got a valid password reset token
    And I have subsequently requested my password is reset using my email address
    When I provide my password reset token and a new password
    And I submit the password reset request
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And it is not cacheable

  Scenario: Using a password reset token after authenticating with a password
    If the user requests a password reset token but then remembers their password and authenticates
    using it then it seems like the tokens should be revoked as they're no longer needed. However,
    a user may be resetting their password because somebody who has discovered their password has
    hijacked their account, so they should be able to use the link to override this.

    Given I have got a valid password reset token
    And I subsequently authenticate using my email address and password
    When I provide my password reset token and a new password
    And I submit the password reset request
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And it is not cacheable

  @extremely_slow
  Scenario: Trying to use an expired password reset token
    Given I have got a valid password reset token
    When I wait for over 24 hours
    And I provide my password reset token and a new password
    And I submit the password reset request
    Then the response indicates that my password reset token is invalid