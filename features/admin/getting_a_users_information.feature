@admin @users @user_info
Feature: Getting a user's information
  As a customer services representative
  I want to be able to access information about customers
  So that I can see infomation that helps me solve their problems

  Background:
    Given I am authenticated as a user in the "customer services representative" role

  Scenario: Getting user information
    Given there is a registered user, call her "Alice"
    When I request admin user information for Alice
    Then the response contains the following user information matching Alice's attributes:
      | ID                       |
      | First Name               |
      | Last Name                |
      | Email Address            |

  Scenario: Getting user information for a user who has changed their email address
    Given there is a registered user, call her "Alice", who has previously changed their email address 3 times
    When I request admin user information for Alice
    Then the the response includes Alice's previous email addresses

  Scenario: Trying to get user information for a nonexistent user
    When I request admin user information for a nonexistent user
    Then the request fails because the user was not found
