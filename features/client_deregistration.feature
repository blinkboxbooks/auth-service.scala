# CP-250
@wip
Feature: Client Deregistration Management
  As a user of registered client
  I want to be able to deregister a client
  So that I can free up some space

  Scenario: Deregister using the same client
    Given I have a registered client
    And I deregister the current client
    Then the deregistration will invalid the token bound to that client
    And I should be left with no client registered

  Scenario: Deregister using different client
    Given I have registered 2 clients
    And I deregister one client
    Then the deregistration will invalid the token bound to that client
    And I should be left with 1 client registered

  Scenario: Deregister client without authenticating
    Given I have a registered client
    And I attempt to deregister the current client while not authenticated
    Then the request fails because I am unauthorised

  Scenario: Deregister a non-existent client
    Given I have a registered client
    And I attempt to deregister a non-existent client
    Then the request fails because client does not exist

  Scenario: Deregister another user client
    Given I have a registered client
    And I attempt to deregister another user client
    Then the request fails because I am unauthorised

  Scenario: Deregister client when none are registered
    Given I do not have any client registered
    When I attempt to deregister a client
    Then request should fail because client is not registered
