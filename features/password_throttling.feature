@authentication @passwords
Feature: Password throttling
  As a user
  I want attempts to authenticate with my email address to be throttled
  So that people cannot obtain access to my account by brute force password guessing

  Scenario: Too many authentication attempts causes the user to be throttled
    Given I have registered an account
    And I have tried to authenticate with the wrong password 5 times within 20 seconds
    When I provide my email address and password
    And I submit the authentication request
    Then the request fails because it has been throttled
    And the response tells me I have to wait for between 1 and 20 seconds to retry

  Scenario: Too many password change attempts causes the user to be throttled
    Given I have registered an account
    And I have tried to change my password with the wrong password 5 times within 20 seconds
    When I provide valid password change details
    And I request my password be changed
    Then the request fails because it has been throttled
    And the response tells me I have to wait for between 1 and 20 seconds to retry

  Scenario: A mixture of authentication and password change attempts causes the user to be throttled
    Both of these actions effectively allow you to check whether a user's password is correct, so the
    throttling should take the combined number of failed attempts into account.

    Given I have registered an account
    And I have tried to authenticate with the wrong password 2 times within 20 seconds
    And I have tried to change my password with the wrong password 3 times within the same 20 seconds
    When I provide my email address and password
    And I submit the authentication request
    Then the request fails because it has been throttled
    And the response tells me I have to wait for between 1 and 20 seconds to retry

  Scenario: Attempts to authenticate with an unregistered email address are throttled
    If we didn't do this you could check whether an email address was registered by checking whether
    attempting to authenticate with it exhibited throttling. Note that this doesn't apply for password
    changes as you need to be authenticated to be able to try and change the password.

    Given I have tried to authenticate with an unregistered email address 5 times within 20 seconds
    When I provide my email address and password
    And I submit the authentication request
    Then the request fails because it has been throttled
    And the response tells me I have to wait for between 1 and 20 seconds to retry

  Scenario: Users can authenticate after waiting for the retry period to elapse
    Given I have registered an account
    And I have tried to authenticate with the wrong password 5 times within 20 seconds
    When I wait for 20 seconds
    And I provide my email address and password
    And I submit the authentication request
    Then the response contains an access token and a refresh token

  Scenario: Users can change their password after waiting for the retry period to elapse
    Given I have registered an account
    And I have tried to change my password with the wrong password 5 times within 20 seconds
    When I wait for 20 seconds
    And I provide valid password change details
    And I request my password be changed
    Then the request succeeds