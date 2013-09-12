Feature: Getting a client's last used date
  As the website client
  I want to know the last date that a device was used.
  So that this information can be displayed to the device's owner when they manage their devices.

  Background:
    Given I have registered an account
    And I have registered a client

  Scenario: An unused client's last used date is the client registration date
    When I wait for two days
    And I request client information for my client with a different refresh token
    Then the response contains client information, excluding the client secret
    And its last used date is two days ago

  Scenario: Client last used date is updated when a client is bound to a refresh token
    When I wait for three days
    And I provide my refresh token and client credentials
    And I submit the access token refresh request
    And I wait for two days
    And I request client information for my client with a different refresh token
    Then the response contains client information, excluding the client secret
    And its last used date is two days ago

  Scenario: Client last used date is updated when a refresh token bound to it is used to generate an access token
    Given I have bound my tokens to my client
    When I wait for three days
    And I provide my refresh token and client credentials
    And I submit the access token refresh request
    And I wait for two days
    And I request client information for my client with a different refresh token
    Then the response contains client information, excluding the client secret
    And its last used date is two days ago