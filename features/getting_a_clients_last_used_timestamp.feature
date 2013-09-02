Feature: Getting a client's last used date and time
  As the website client
  I want to know the last date and time that a device was used.
  So that this information can be displayed to the device's owner when they manage their devices.

  Background:
    Given I have registered an account
    And I have registered a client

  Scenario: Request information for a client
    When I request client information for my client
    Then the response contains client information, excluding the client secret
    And it contains a record of the time the client was last used