@refresh_tokens
Feature: Refreshing an access token
  As a client application
  I want to be able to refresh an access token
  So that I can continue to call identity-based service

  Background:
    Given I have registered an account

  Scenario: Refreshing an access token using a refresh token
    Given I have provided my refresh token
    When I submit the access token refresh request
    Then the response contains an access token 