@users @user_info
Feature: Updating a user's information
  As a user
  I want to be able to update information about myself
  So that I can keep my details up to date

  Background:
    Given I have registered an account

  Scenario: Updating email address
    When I change my email address
    And I request my user information be updated
    Then the response contains complete user information matching my new details
    And it is not cacheable

  Scenario: Updating first name and last name
    When I change my first name to "Bob"
    And I change my last name to "Smith"
    And I request my user information be updated
    Then the response contains complete user information matching my new details
    And it is not cacheable

  Scenario: Updating marketing preferences
    When I change whether I allow marketing communications
    And I request my user information be updated
    Then the response contains complete user information matching my new details
    And it is not cacheable

  Scenario: Trying to change acceptance of terms and conditions
    When I change whether I accepted terms and conditions
    And I request my user information be updated
    Then the request fails because it is invalid

  Scenario: Trying to update user information without authorisation
    When I request my user information be updated, without my access token
    Then the request fails because I am unauthorised

  Scenario: Trying to update user information for a different user
    For security reasons we don't distinguish between a user that doesn't exist and a user that 
    does exist but is not the current user. In either case we say it was not found.
    
    Given another user has registered an account
    When I request the other user's information be updated
    Then the request fails because the user was not found