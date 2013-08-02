@clients @client_info
Feature: Getting a client's information
  As a user
  I want to be able to get information about my current client
  So that I can use and display the details

  Background:
    Given I have registered an account
    And I have registered a client

  Scenario: Getting client information
    Given I have provided my access token
    When I submit a client information request for the current client
    Then the response contains client information, excluding the client secret
    And it is not cacheable

  Scenario: Trying to get client information without authorisation
    Given I have not provided my access token
    When I submit a client information request for the current client
    Then the request fails because I am unauthorised

  Scenario: Trying to get client information for a nonexistent client
    Given I have provided my access token
    When I submit a client information request for a nonexistent client
    Then the request fails because the client was not found

  # TODO: This step needs to be present, but is hard to write the way the tests are
  #       currently structured. Need to look at restructuring the tests to allow it.
  Scenario: Trying to get client information for another user's client
    For security reasons we don't distinguish between a client that doesn't exist and a client that 
    does exist but the user isn't allowed to access. In either case we say it was not found.
    Given I have provided my access token
    When I submit a client information request for another user's client
    Then the request fails because the client was not found