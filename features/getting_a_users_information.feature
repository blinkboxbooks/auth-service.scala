@users @user_info
Feature: Getting a user's information
  As an application
  I want to be able to get information about the authenticated user
  So that I can use and display their details

  Background:
    Given I have registered an account

  Scenario: Getting user information
    Given I have provided my access token
    When I submit the user information request
    Then the response contains complete user information matching the registration details
    And it is not cacheable

  Scenario: Trying to get user information without authorisation
    Given I have not provided my access token
    When I submit the user information request
    Then the request fails because I am unauthorised

  Scenario: Trying to get user information for a different user
    Given I have provided my access token
    When I submit the user information request for a different user
    Then the request fails because this is forbidden