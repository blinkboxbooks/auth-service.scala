@authentication @refresh_tokens
Feature: Refreshing an access token
  As a client application
  I want to be able to refresh an access token
  So that I can continue to call identity-based service

  Background:
    Given I have registered an account

  Scenario: Refreshing an access token using a refresh token
    When I provide my refresh token
    And I submit the access token refresh request
    Then the response contains an access token
    And it contains basic user information matching my details
    And it is not cacheable

  Scenario: Trying to refresh an access token without a refresh token
    When I do not provide my refresh token
    And I submit the access token refresh request
    Then the request fails because it is invalid

  Scenario: Trying to refresh an access token with a revoked refresh token
    Given I have revoked my refresh token
    When I provide my refresh token
    And I submit the access token refresh request
    Then the response indicates that my refresh token is invalid

  Scenario: Trying to refresh an access token with a nonexistent refresh token
    When I provide a nonexistent refresh token
    And I submit the access token refresh request
    Then the response indicates that my refresh token is invalid

  Scenario: Binding a refresh token to a client using client credentials
    Given I have registered a client
    When I provide my refresh token and client credentials
    And I submit the access token refresh request
    Then the response contains an access token
    And it contains basic user information matching my details
    And it contains client information, excluding the client secret
    And it is not cacheable

  Scenario: Refreshing an access token using a refresh token that is bound to a client, with client credentials
    Given I have registered a client
    And I have bound my tokens to my client
    When I provide my refresh token and client credentials
    And I submit the access token refresh request
    Then the response contains an access token
    And it contains basic user information matching my details
    And it contains client information, excluding the client secret
    And it is not cacheable

  Scenario: Trying to refresh an access token using a refresh token that is bound to a client, without client credentials
    Given I have registered a client
    And I have bound my tokens to my client
    When I provide my refresh token
    But I do not provide my client credentials
    And I submit the access token refresh request
    Then the response indicates that the client credentials are incorrect

