@live @elevations
Feature: Access token elevation
  As a program that operates on personal user information
  I want to be able to get information about an access token
  So that I check whether it is currently valid and its elevation level

  Background:
    Given I have registered an account

  Scenario: Authenticating with email address and password gives critical elevation
    Given I obtain an access token using my email address and password
    When I request information about the access token
    Then the response contains access token information
    And its elevation is critical
    And the critical elevation expires ten minutes from now

  @slow
  Scenario: After ten minutes of non-elevated activity, elevation drops to elevated
    Given I obtain an access token using my email address and password
    And I wait for just over ten minutes
    When I request information about the access token
    Then the response contains access token information
    And it is elevated

  @extremely_slow
  Scenario: After one day of non-elevated activity, elevation drops to none
    Given I obtain an access token using my email address and password
    And I wait for over one day
    When I request information about the access token
    Then the response contains access token information
    And it is not elevated

  @slow
  Scenario: Critically elevated session lifetime can be extended
    Given I obtain an access token using my email address and password
    And I wait for nine minutes
    When I submit the access token refresh request
    And I request information about the access token
    Then the response contains access token information
    And it is critically elevated
    And the elevation expires ten minutes from now

  @slow
  Scenario: Extending elevation while elevated only extends elevated session (not critically elevated session)
    Given I obtain an access token using my email address and password
    And I wait for just over ten minutes
    When I submit the access token refresh request
    And I request information about the access token
    Then the response contains access token information
    And it is elevated
    And the elevation expires one day from now

  @extremely_slow
  Scenario: Elevated session lifetime cannot be extended when it has already ended
    Given I have a non-elevated access token
    When I request that my elevated session be extended
    Then the request fails because I am unauthorised
    And the reason is that my identity is considered unverified

  @slow
  Scenario: Refreshing a elevated access token does not extend the elevated session lifetime
    Refreshing an access token is orthogonal to the concept of elevation, so refreshing a
    token doesn't affect the elevated session in any way.
    
    Given I obtain an access token using my email address and password
    And I wait for two minutes
    When I submit the access token refresh request
    And I request information about the access token
    Then the response contains access token information
    And its elevation is critical
    And the elevation expires eight minutes from now

  @slow
  Scenario: Refreshing an elevated access token does not grant elevation
    Refreshing an access token is orthogonal to the concept of elevation. The act of
    refreshing doesn't prove your identity, so doesn't grant elevated access.

    Given I have an elevated access token
    When I submit the access token refresh request
    And I request information about the access token
    Then the response contains access token information
    And it is elevated

  @extremely_slow
  Scenario: Refreshing a non-elevated access token does not grant elevation
    Refreshing an access token is orthogonal to the concept of elevation. The act of
    refreshing doesn't prove your identity, so doesn't grant elevated access.

    Given I have a non-elevated access token
    When I submit the access token refresh request
    And I request information about the access token
    Then the response contains access token information
    And it is not elevated