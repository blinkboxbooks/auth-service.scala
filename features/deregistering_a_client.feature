@clients @deregistration @client_deregistration
Feature: Deegistering a client
  As a user
  I want to be able to register my client
  So that clients I no longer use/own are able to act on my behalf

  Background:
    Given I have registered an account
    And I have registered a client
    And I have bound my tokens to my client

  Scenario: Deregistering my current client
    Deregistering a client also revokes the tokens that are bound to that client, so if you deregister
    your current client then your tokens will no longer be valid.

    When I request that my current client be deregistered
    Then the request succeeds
    And my refresh token and access token are invalid because they have been revoked
    And I have got no registered clients

  Scenario: Deregistering one of my other clients
    If you deregister another client though, it has no effect on your tokens. This is because the 
    other client is a separate concern, and you might be deregistering it because it was lost or
    stolen from another legitimate client that you don't want to be signed out of.
    
    Given I have registered another client
    When I request that my other client be deregistered
    Then the request succeeds
    And my refresh token and access token are valid
    And I have got one registered client

  Scenario: Trying to deregister a client without authorisation
    When I request that my current client be deregistered, without my access token
    Then the request fails because I am unauthorised

  Scenario: Trying to deregister a nonexistent client
    When I request that a nonexistent client be deregistered
    Then the request fails because the client was not found

  Scenario: Trying to deregister an already deregistered client
    Given I have deregistered my current client
    When I request that my current client be deregistered
    Then the request fails because the client was not found

  Scenario: Trying to deregister another user's client
    For security reasons we don't distinguish between a client that doesn't exist and a client that 
    does exist but the user isn't allowed to access. In either case we say it was not found.

    Given another user has registered an account
    And another user has registered a client
    When I request that the other user's client be deregistered
    Then the request fails because the client was not found