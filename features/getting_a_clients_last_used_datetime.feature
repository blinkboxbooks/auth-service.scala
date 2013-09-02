Feature: Getting a client's last used timestamp
  As the website client
  I want to know the last date and time that a device authenticated
  So that this information can be displayed to the device's owner when they manage their devices.

  Background:
    Given I have registered an account
    And I have registered a client

  Scenario: Getting the last used timestamp for a client which has only been registered
    When I request client information for my client
    Then the response contains client information, excluding the client secret
    And its last used timestamp is the same as my client registration timestamp
    And it is not cacheable

  Scenario: Getting the last used timestamp for a client which has requested access tokens
    Given I wait 3 seconds
    And I refresh my access token
    When I request client information for my client
    Then the response contains client information, excluding the client secret
    And its last used timestamp is the same as my access token creation timestamp
    And it is not cacheable