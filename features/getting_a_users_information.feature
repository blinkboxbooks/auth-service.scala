@users @user_info
Feature: Getting a user's information
  As an application
  I want to be able to get information about the authenticated user
  So that I can use and display their details

  Background:
    Given I have registered an account

  Scenario: Getting user information
    Given I have provided my access token
    When I submit a user information request for the current user
    Then the response contains complete user information matching the registration details
    And it is not cacheable

  Scenario: Trying to get user information without authorisation
    Given I have not provided my access token
    When I submit a user information request for the current user
    Then the request fails because I am unauthorised

  Scenario: Trying to get user information for a nonexistent user
    Given I have provided my access token
    When I submit a user information request for a nonexistent user
    Then the request fails because the user was not found

  Scenario: Trying to get user information for a different user
    For security reasons we don't distinguish between a user that doesn't exist and a user that 
    does exist but is not the current user. In either case we say it was not found.
    Given I have provided my access token
    When I submit a user information request for a different user
    Then the request fails because the user was not found