@clients @client_info
Feature: Listing client information
  As a user
  I want to be able to list information about all my clients
  So that I can use and display the details

  Background:
    Given I have registered an account

  Scenario: Listing client information for no clients
    Given I have registered no clients
    When I request client information for all my clients
    Then the response contains a list of 0 client's information
    And it is not cacheable

  Scenario: Listing client information for multiple clients
    Given I have registered 3 clients
    When I request client information for all my clients
    Then the response contains a list of 3 client's information, excluding the client secret
    And it is not cacheable

  Scenario: Trying to list client information without authorisation
    # RFC 6750 § 3.1:
    #   If the request lacks any authentication information (e.g., the client
    #   was unaware that authentication is necessary or attempted using an
    #   unsupported authentication method), the resource server SHOULD NOT
    #   include an error code or other error information.

    When I request client information for all my clients, without my access token
    Then the request fails because I am unauthorised
    And the response does not include any error information
