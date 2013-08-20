@clients @client_info
Feature: Getting a client's information
  As a user
  I want to be able to get information about my client
  So that I can use and display the details

  Background:
    Given I have registered an account
    And I have registered a client
    And I have bound my tokens to my client

  Scenario: Getting my current client's information
    When I request client information for my client
    Then the response contains client information, excluding the client secret
    And it is not cacheable

  Scenario: Getting one of my other client's information
    Users can get the information for any of their clients from anywhere as long as
    they are authenticated; it doesn't have to be done from the same client. This is so 
    that they can manage all their clients from a central location.

    Given I have registered another client
    When I request client information for my other client
    Then the response contains client information, excluding the client secret
    And it is not cacheable

  Scenario: Trying to get client information without authorisation
    When I request client information for my client, without my access token
    Then the request fails because I am unauthorised

  Scenario: Trying to get client information for a nonexistent client
    When I request client information for a nonexistent client
    Then the request fails because the client was not found

  Scenario: Trying to get client information for another user's client
    For security reasons we don't distinguish between a client that doesn't exist and a client that 
    does exist but the user isn't allowed to access. In either case we say it was not found.
    
    Given another user has registered an account
    And another user has registered a client
    When I request client information for the other user's client
    Then the request fails because the client was not found