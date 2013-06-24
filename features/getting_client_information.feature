@clients
Feature: Registering a client
  As a client
  I want to be able to get information about myself
  So that I can make sure the server is up to date

  Background:
    Given I have registered an account
    And I have registered a client

  Scenario: Getting client information
    Given I have provided my client access token
    When I submit the client information request
    Then the response contains client information, including a client secret

  Scenario: Trying to get client information without authorisation
    Given I have not provided my client access token
    When I submit the client information request
    Then the response indicates that I am unauthorised

  Scenario: Trying to get client information for a different client
    Given I have provided the access token for a different client
    When I submit the client information request
    Then the response indicates that this is forbidden
