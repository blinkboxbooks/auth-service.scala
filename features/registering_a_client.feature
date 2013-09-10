@clients @registration @client_registration
Feature: Registering a client
  As a user
  I want to be able to register my client
  So that I can use client-dependent services

  Background:
    Given I have registered an account

  Scenario: Registering a client with a name and model
    When I provide a client name
    And I provide a client model
    And I submit the client registration request
    Then the response contains client information, including a client secret
    And the client name matches the provided name
    And the client model matches the provided model
    And it is not cacheable

  Scenario: Registering a client with details containing international characters
    When I provide a client name of "Iñtërnâtiônàlizætiøn"
    And I provide a client model of "中国扬声器可以阅读本"
    And I submit the client registration request
    Then the response contains client information, including a client secret
    And the client name matches the provided name
    And the client model matches the provided model
    And it is not cacheable

  Scenario: Registering a client without a name
    When I provide a client model
    But I do not provide a client name
    And I submit the client registration request
    Then the response contains client information, including a client secret
    And the client model matches the provided model
    And the client name is "Unnamed Client"
    And it is not cacheable

  Scenario: Registering a client without a model
    When I provide a client name
    But I do not provide a client model
    And I submit the client registration request
    Then the response contains client information, including a client secret
    And the client name matches the provided name
    And the client model is "Unknown Device"
    And it is not cacheable

  Scenario: Trying to register a client without user authorisation
    # RFC 6750 § 3.1:
    #   If the request lacks any authentication information (e.g., the client
    #   was unaware that authentication is necessary or attempted using an
    #   unsupported authentication method), the resource server SHOULD NOT
    #   include an error code or other error information.

    When I submit a client registration request, without my access token
    Then the request fails because I am unauthorised
    And the response does not include any error information

  Scenario: Trying to register a client with an empty name
    Not providing a name is OK because it's optional, but providing an empty name means that an
    invalid name has been provided, which should return an error.
    
    When I provide a client name of ""
    And I submit the client registration request
    Then the request fails because it is invalid

  Scenario: Trying to register a client with an empty model
    Not providing a model is OK because it's optional, but providing an empty model means that an
    invalid model has been provided, which should return an error.
    
    When I provide a client model of ""
    And I submit the client registration request
    Then the request fails because it is invalid

  Scenario: Trying to register a client with a name that is too long
    The client name can't be more than 50 characters.
    
    When I provide a client name of "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
    And I submit the client registration request
    Then the request fails because it is invalid

  Scenario: Trying to register a client with a model that is too long
    The client model can't be more than 50 characters.
    
    When I provide a client model of "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
    And I submit the client registration request
    Then the request fails because it is invalid

  Scenario: Trying to register more than the allowed number of clients
    Given I have registered 12 clients
    When I submit a client registration request
    Then the request fails because it is invalid
    And the reason is that the client limit has been reached