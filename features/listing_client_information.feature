@clients @client_info @wip
Feature: Listing client information
  As a user
  I want to be able to list information about all my clients
  So that I can use and display the details

  Scenario: Listing client information for no clients
    Given I have registered an account
    And I have provided my access token
    When I submit a client information request for all my clients
    Then the response contains a list of 0 client's information
    And it is not cacheable

  Scenario: Listing client information for multiple clients
    Given I have registered an account
    And I have registered 3 clients
    And I have provided my access token
    When I submit a client information request for all my clients
    Then the response contains a list of 3 client's information, excluding the client secret
    And it is not cacheable

  Scenario: Trying to list client information without authorisation
    Given I have registered an account
    And I have registered a client
    And I have not provided my access token
    When I submit a client information request for the current client
    Then the request fails because I am unauthorised