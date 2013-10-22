Feature: Getting a client's last used date
  As the website client
  I want to know the last date that a device was used.
  So that this information can be displayed to the device's owner when they manage their devices.

  Background:
    Given I have registered an account

  @extremely_slow
  Scenario: An unused client's last used date is the client registration date
    Given I have registered a client
    When I wait for two days
    And I request client information for my client, using an access token that is not bound to it
    Then the response contains client information, excluding the client secret
    And its last used date is two days ago

  @extremely_slow
  Scenario: Client last used date is updated when a refresh token is bound to a client
    Given I have registered a client
    When I wait for three days
    And I bind my tokens to my client
    And I wait for two days
    And I request client information for my client, using an access token that is not bound to it
    Then the response contains client information, excluding the client secret
    And its last used date is two days ago

  @extremely_slow
  Scenario: Client last used date is updated when a refresh token bound to it is used to generate an access token
    Given I have registered a client
    And I have bound my tokens to my client
    When I wait for three days
    And I bind my tokens to my client
    And I wait for two days
    And I request client information for my client, using an access token that is not bound to it
    Then the response contains client information, excluding the client secret
    And its last used date is two days ago

  Scenario: Listing clients includes last used dates for each client
    Given I have registered 3 clients in total
    When I request client information for all my clients
    Then the response contains a list of 3 client's information, excluding the client secret
    And each client has a last used date