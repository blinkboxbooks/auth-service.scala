# CP-250
@wip
Feature: Client Deregistration Management
  As a user of registered client
  I want to be able to deregister a client
  So that I can free up some space

  Scenario: Deregister using the same client
    Given I have registered a client
    And I deregister the current client
    Then my refresh token and access token are invalid because they have been revoked
    And I have got no clients registered

  Scenario: Deregister using different client
    Given I have registered 2 clients
    And I deregister one client
    Then my refresh token and access token are invalid because they have been revoked
    And I should be left with 1 client registered

  Scenario: Deregister client without authenticating
    Given I have a registered client
    When I request that my current client be deregistered, without my access token
    Then the request fails because the client was not found

  Scenario: Deregister a non-existent client
    Given I have a registered client
    And I attempt to deregister a non-existent client
    Then the request fails because the client was not found

  Scenario: Deregister another user client
    Given another user has registered an account
    And another user has registered a client
    When I request client information for the other user's client
    Then the request fails because I am unauthorised

  Scenario: Deregister client when none are registered
    Given I do not have any client registered
    When I attempt to deregister a client
    Then the request fails because the client was not found
