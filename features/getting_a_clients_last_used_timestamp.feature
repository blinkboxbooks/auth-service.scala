Feature: Getting a client's last used date
  As the website client
  I want to know the last date that a device was used.
  So that this information can be displayed to the device's owner when they manage their devices.

  Background:
    Given I have registered an account
    And I have registered a client

  Scenario: An unused client's last used date is the client registration date
    When I request client information for my client
    Then the response contains client information, excluding the client secret
    And its last used date is when the client was registered
    And it is not cacheable

  Scenario: Client last used date is updated when a client is bound to a refresh token
    When I wait three seconds
    And I provide my refresh token and client credentials
    And I submit the access token refresh request
    And I wait two seconds
    And I request client information for my client
    Then the response contains client information, excluding the client secret
    And its last used date is two seconds ago
    And it is not cacheable

  Scenario: Client last used date is updated when a refresh token bound to it is used to generate an access token
    Given I have bound my tokens to my client
    When I wait three seconds
    And I provide my refresh token and client credentials
    And I submit the access token refresh request
    And I wait two seconds
    And I request client information for my client
    Then the response contains client information, excluding the client secret
    And its last used date is two seconds ago
    And it is not cacheable