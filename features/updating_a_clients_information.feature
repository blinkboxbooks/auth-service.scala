@clients @client_info
Feature: Updating a client's information
  As a user
  I want to be able to update information about my client
  So that I can keep its details up to date

  Background:
    Given I have registered an account
    And I have registered a client
    And I have bound my tokens to my client

  Scenario: Updating my current client's name and model
    When I change my client's name to "My iPad Mini"
    And I change my client's model to "Apple iPad Mini"
    And I request my client's information be updated
    Then the response contains client information, excluding the client secret
    And the client name is "My iPad Mini"
    And the client model is "Apple iPad Mini"
    And it is not cacheable

  Scenario: Updating one of my other client's name and model
    Users can update the information for any of their clients from anywhere as long as
    they are authenticated; it doesn't have to be done from the same client. This is so 
    that they can manage all their clients from a central location.

    Given I have registered another client
    When I change my other client's name to "My Nexus 7"
    When I change my other client's model to "Google Nexus 7"
    And I request my other client's information be updated
    Then the response contains client information, excluding the client secret
    And the client name is "My Nexus 7"
    And the client model is "Google Nexus 7"
    And it is not cacheable

  Scenario: Trying to update client information without authorisation
    When I request my client's information be updated, without my access token
    Then the request fails because I am unauthorised

  Scenario: Trying to update client information for a different user's client
    For security reasons we don't distinguish between a user that doesn't exist and a user that 
    does exist but is not the current user. In either case we say it was not found.
    
    Given another user has registered an account
    And another user has registered a client
    When I request the other user's client's information be updated
    Then the request fails because the client was not found