@authentication @passwords
Feature: Password authentication
  As a user
  I want to be able to authenticate with my email address and password
  So that I can use services that require my identity

  Background:
    Given I have registered an account

  Scenario: Authenticating with valid credentials
    When I provide my email address and password
    And I submit the authentication request
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And it is not cacheable

  Scenario: Authentication with email address in a different case to the one used when registering
    When I provide my email address and password
    But the email address is in a different case to the one I used to register
    And I submit the authentication request
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And it is not cacheable  

  Scenario: Trying to authenticate without a password
    When I provide my email address
    But I do not provide my password
    And I submit the authentication request
    Then the request fails because it is invalid

  Scenario: Trying to authenticate with an incorrect password
    When I provide my email address and password
    But the password is incorrect
    And I submit the authentication request
    Then the response indicates that my credentials are incorrect

  Scenario: Trying to authenticate with an unregistered email address
    When I provide my email address and password
    But the email address is different from the one I used to register
    And I submit the authentication request
    Then the response indicates that my credentials are incorrect

  Scenario: Authenticating with valid credentials and client credentials
    Given I have registered a client
    When I provide my email address, password and client credentials
    And I submit the authentication request
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    # TODO: And it contains client information, excluding the client secret
    And it is not cacheable

  Scenario: Trying to authenticate with valid credentials but a missing client secret
    Given I have registered a client
    When I provide my email address, password and client credentials
    But I do not provide my client secret
    And I submit the authentication request
    Then the response indicates that the client credentials are incorrect

  Scenario: Trying to authenticate with valid credentials but an incorrect client secret
    Given I have registered a client
    When I provide my email address, password and client credentials
    But the client secret is incorrect
    And I submit the authentication request
    Then the response indicates that the client credentials are incorrect

  Scenario: Trying to authenticate with valid credentials but another user's client credentials
    Given another user has registered an account
    And another user has registered a client
    When I provide my email address and password
    But I provide the other user's client credentials
    And I submit the authentication request
    Then the response indicates that the client credentials are incorrect