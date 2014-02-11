@messaging
Feature: User authenticated events
  As a system in the blinkbox books platform
  I want to know when users have authenticated
  So that I can track and respond to their activity

  Scenario: A user authenticates with username and password
    Given there is a registered user, call her "Alice"
    When Alice obtains an access token using her email address and password
    Then a user authenticated message is sent
    And the message contains Alice's details:
      | id                             |
      | username                       |
      | first name                     |
      | last name                      |
      | allow marketing communications |
    And the message contains a user event timestamp

  Scenario: A user authenticates with username, password and client credentials
    Given there is a registered user, call her "Alice"
    And Alice has registered a client
    When Alice obtains an access token using her email address, password and client credentials
    Then a user authenticated message is sent
    And the message contains Alice's details:
      | id                             |
      | username                       |
      | first name                     |
      | last name                      |
      | allow marketing communications |
    And the message contains Alice's client's details:
      | id    |
      | name  |
      | brand |
      | model |
      | os    |
    And the message contains a user event timestamp

  Scenario: A user refreshes their access token using a refresh token
    Given there is a registered user, call her "Alice"
    When Alice obtains an access token using her refresh token
    Then a user authenticated message is sent
    And the message contains Alice's details:
      | id                             |
      | username                       |
      | first name                     |
      | last name                      |
      | allow marketing communications |
    And the message contains a user event timestamp

  Scenario: A user refreshes their access token using a refresh token and client credentials
    Given there is a registered user, call her "Alice"
    And Alice has registered a client
    When Alice obtains an access token using her refresh token and client credentials
    Then a user authenticated message is sent
    And the message contains Alice's details:
      | id                             |
      | username                       |
      | first name                     |
      | last name                      |
      | allow marketing communications |
    And the message contains Alice's client's details:
      | id    |
      | name  |
      | brand |
      | model |
      | os    |
    And the message contains a user event timestamp

  Scenario: A user resets their password using a password reset token
    Given there is a registered user, call her "Alice"
    And Alice has got a valid password reset token
    When Alice obtains an access token using her password reset token
    Then a user authenticated message is sent
    And the message contains Alice's details:
      | id                             |
      | username                       |
      | first name                     |
      | last name                      |
      | allow marketing communications |
    And the message contains a user event timestamp

  Scenario: A user refreshes their access token using a refresh token and client credentials
    Given there is a registered user, call her "Alice"
    And Alice has registered a client
    And Alice has got a valid password reset token
    When Alice obtains an access token using her password reset token and client credentials
    Then a user authenticated message is sent
    And the message contains Alice's details:
      | id                             |
      | username                       |
      | first name                     |
      | last name                      |
      | allow marketing communications |
    And the message contains Alice's client's details:
      | id    |
      | name  |
      | brand |
      | model |
      | os    |
    And the message contains a user event timestamp
