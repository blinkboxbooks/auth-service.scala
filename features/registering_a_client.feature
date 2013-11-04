@clients @registration @client_registration
Feature: Registering a client
  As a user
  I want to be able to register my client
  So that I can use client-dependent services

  Background:
    Given I have registered an account

  Scenario: Registering a client with all details, within critical elevation period
    Given I have a critically elevated access token
    When I provide the client registration details:
      | name  | Test Device |
      | brand | Test Brand  |
      | model | Test Model  |
      | OS    | Test OS     |
    And I submit the client registration request
    Then the response contains client information, including a client secret
    And the client details match the provided details
    And it is not cacheable

  @extremely_slow
  Scenario Outline: Registering a client with all details, outside critical elevation period
    Given I have <elevation_level> access token
    When I provide the client registration details:
      | name  | Test Device |
      | brand | Test Brand  |
      | model | Test Model  |
      | OS    | Test OS     |
    And I submit the client registration request
    Then the request fails because I am unauthorised
    And the response includes low elevation level information

    Examples:
      | elevation_level |
      | an elevated     |
      | a non-elevated  |

  Scenario: Registering a client with details containing international characters
    When I provide the client registration details:
      | name  | Iñtërnâtiônàlizætiøn         |
      | brand | 中国扬声器可以阅读本             |
      | model | ٩(-̮̮̃-̃)۶ ٩(●̮̮̃•̃)۶ ٩(͡๏̯͡๏)۶ ٩(-̮̮̃•̃). |
      | OS    | ЌύБЇ                         |
    And I submit the client registration request
    Then the response contains client information, including a client secret
    And the client details match the provided details
    And it is not cacheable

  Scenario Outline: Registering a client with missing required details
    When I provide a client model
    But I do not provide a client <detail>
    And I submit the client registration request
    Then the request fails because it is invalid

    Examples:
      | detail |
      | name   |
      | brand  |
      | model  |
      | OS     |

  Scenario: Trying to register a client without user authorisation
    When I submit a client registration request, without my access token
    Then the request fails because I am unauthorised
    And the response does not include any error information

  Scenario Outline: Trying to register a client with empty optional details
    Not providing an optional detail is OK, but if the detail is provided it can't be empty.

    When I provide a client <detail> of ""
    And I submit the client registration request
    Then the request fails because it is invalid

    Examples:
      | detail |
      | name   |
      | brand  |
      | model  |
      | OS     |

  Scenario Outline: Trying to register a client with a detail that is too long
    The client details can't be more than 50 characters.

    When I provide a client <detail> of "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
    And I submit the client registration request
    Then the request fails because it is invalid

    Examples:
      | detail |
      | name   |
      | brand  |
      | model  |
      | OS     |

  Scenario: Trying to register more than the allowed number of clients
    Given I have registered 12 clients in total
    When I submit a client registration request
    Then the request fails because it is invalid
    And the reason is that the client limit has been reached