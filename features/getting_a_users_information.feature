@users @user_info @wip
Feature: Getting a user's information
  As a user
  I want to be able to get information about myself
  So that I can check whether my details are up to date

  Background:
    Given I have registered an account

  Scenario: Getting user information
    Given I have provided my access token
    When I submit a user information request for myself
    Then the response contains complete user information matching my details
    And it is not cacheable

  Scenario: Trying to get user information without authorisation
    Given I have not provided my access token
    When I submit a user information request for myself
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