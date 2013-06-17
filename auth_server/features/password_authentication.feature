@authentication @passwords
Feature: Password authentication
  As a user
  I want to be able to authenticate with my email address and password
  So that I can use services that require my identity

  Background:
    Given I have registered an account

  Scenario: Authenticating with valid credentials
    Given I have provided my email address and password
    When I submit the authentication request
    Then the response contains an access token and a refresh token

  Scenario: Trying to authenticate without a password
    Given I have provided my email address
    But the password is missing
    When I submit the authentication request
    Then the response indicates that the request was invalid

  Scenario: Trying to authenticate with an incorrect password
    Given I have provided my email address and password
    But the password is incorrect
    When I submit the authentication request
    Then the response indicates that my credentials are incorrect

  Scenario: Trying to authenticate with an unregistered email address
    Given I have provided my email address and password
    But the email address is different from the one I used to register
    When I submit the authentication request
    Then the response indicates that my credentials are incorrect

  Scenario: Authenticating with valid credentials and client credentials
    Given I have registered a client
    And I have provided my email address and password
    When I submit the authentication request
    Then the response contains an access token and a refresh token

  Scenario: Trying to authenticate with valid credentials but a missing client secret
    Given I have registered a client
    And I have provided my email address and password
    But the client secret is missing
    When I submit the authentication request
    Then the response indicates that the client credentials are incorrect

  Scenario: Trying to authenticate with valid credentials but an incorrect client secret
    Given I have registered a client
    And I have provided my email address and password
    But the client secret is incorrect
    When I submit the authentication request
    Then the response indicates that the client credentials are incorrect