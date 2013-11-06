@clients @client_info
Feature: Updating a client's information
  As a user
  I want to be able to update information about my client
  So that I can keep its details up to date

  Background:
    Given I have registered an account
    And I have registered a client
    And I have bound my tokens to my client

  Scenario: Updating my current client's details, within critical elevation period
    Given I have a critically elevated access token
    When I change my client's details to:
      | name  | Updated Device |
      | brand | Updated Brand  |
      | model | Updated Model  |
      | OS    | Updated OS     |
    And I request my client's information be updated
    Then the response contains client information, excluding the client secret
    And the client details match the provided details
    And it is not cacheable
    And the critical elevation got extended

  @extremely_slow
  Scenario Outline: Updating my current client's name and model, outside critical elevation period
    Given I have <elevation_level> access token
    When I change my client's details to:
      | name  | Updated Device |
      | brand | Updated Brand  |
      | model | Updated Model  |
      | OS    | Updated OS     |
    And I request my client's information be updated
    Then the request fails because I am unauthorised
    And the response includes low elevation level information

    Examples:
      | elevation_level |
      | an elevated     |
      | a non-elevated  |

  Scenario: Updating one of my other client's name and model, within critical elevation period
    Users can update the information for any of their clients from anywhere as long as
    they are authenticated; it doesn't have to be done from the same client. This is so
    that they can manage all their clients from a central location.

    Given I have registered another client
    And I have a critically elevated access token
    When I change my other client's details to:
      | name  | Updated Device |
      | brand | Updated Brand  |
      | model | Updated Model  |
      | OS    | Updated OS     |
    And I request my other client's information be updated
    Then the response contains client information, excluding the client secret
    And the other client details match the provided details
    And it is not cacheable
    And the critical elevation got extended

  @extremely_slow
  Scenario Outline: Updating one of my other client's name and model, outside critical elevation period
    Users can update the information for any of their clients from anywhere as long as
    they are authenticated; it doesn't have to be done from the same client. This is so
    that they can manage all their clients from a central location.

    Given I have registered another client
    And I have <elevation_level> access token
    When I change my other client's details to:
      | name  | Updated Device |
      | brand | Updated Brand  |
      | model | Updated Model  |
      | OS    | Updated OS     |
    And I request my other client's information be updated
    Then the request fails because I am unauthorised
    And the response includes low elevation level information

    Examples:
      | elevation_level |
      | an elevated     |
      | a non-elevated  |

  Scenario: Trying to update client information without authorisation
    When I request my client's information be updated, without my access token
    Then the request fails because I am unauthorised
    And the response does not include any error information

  Scenario: Trying to update client information
    When I do not change my client's details
    And I request my client's information be updated
    Then the request fails because it is invalid

  Scenario: Trying to update client information for a different user's client
    For security reasons we don't distinguish between a user that doesn't exist and a user that
    does exist but is not the current user. In either case we say it was not found.

    Given another user has registered an account
    And another user has registered a client
    When I request the other user's client's information be updated
    Then the request fails because the client was not found