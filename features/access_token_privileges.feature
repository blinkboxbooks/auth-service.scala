@live @privileges
Feature: Access token privileges
  As a program that operates on personal user information
  I want to be able to get information about an access token
  So that I check whether it is currently valid and its privilege level

  Background:
    Given I have registered an account

  Scenario: Authenticating with email address and password gives critical privileges
    Given I obtain an access token using my email address and password
    When I request information about the access token
    Then it is elevated to critical
    And the critical privileges expire ten minutes from now

  @slow
  Scenario: After ten minutes of non-privileged activity, privileges drop to none
    Given I obtain an access token using my email address and password
    And I wait for over ten minutes
    When I request information about the access token
    Then it is not elevated

  @slow
  Scenario: Privileged session lifetime can be extended
    Given I obtain an access token using my email address and password
    And I wait for nine minutes
    When I submit the access token refresh request
    Then I request information about the access token
    And it is elevated to critical
    And the critical privileges expire ten minutes from now

  @slow
  Scenario: Privileged session lifetime cannot be extended when it has already ended
    Given I obtain an access token using my email address and password
    And I wait for over ten minutes
    When I request that my elevated session be extended
    Then the request fails because I am unauthorised
    And the reason is that my identity is considered unverified

  @slow
  Scenario: Refreshing a privileged access token does not extend the privileged session lifetime
    Refreshing an access token is orthogonal to the concept of privileges, so refreshing a
    token doesn't affect the privileged session in any way.
    
    Given I obtain an access token using my email address and password
    And I wait for two minutes
    When I submit the access token refresh request
    And I request information about the access token
    Then it is elevated to critical
    And the critical privileges expire eight minutes from now

  @slow
  Scenario: Refreshing a non-privileged access token does not grant privileges
    Refreshing an access token is orthogonal to the concept of privileges. The act of
    refreshing doesn't prove your identity, so doesn't grant privileges.

    Given I have a non-privileged access token
    When I submit the access token refresh request
    And I request information about the access token
    Then it is not elevated