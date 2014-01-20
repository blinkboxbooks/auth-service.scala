@dev
Feature: Search for users
  As a customer services representative
  I want to be able to find the users who have problems
  So that I can see information about them

  Background:
    Given I am authenticated as a user in the "customer services representative" role
    And there is a registered user, call her "Alice"
    And there is a registered user, call him "Bob", who has previously changed his email address

  Scenario: Users can be found by email address
    When I search for users with Alice's email address
    Then the response is a list containing one user
    And the first user matches Alice's attributes

  Scenario: Users can be found by historical email address
    When I search for users with Bob's old email address
    Then the response is a list containing one user
    And the first user matches Bob's attributes

  Scenario: Users can be found by first name and last name
    When I search for users with Alice's first name and last name
    Then the response is a list containing one user
    And the first user matches Alice's attributes

  Scenario: Users can be found by identifier
    When I search for users with Bob's user id
    Then the response is a list containing one user
    And the first user matches Bob's attributes

  @negative
  Scenario Outline: Searching for users with unregistered attributes returns no results
    When I search for users with an unregistered <attributes>
    Then the response is a list containing no users

    Examples:
      | attributes          |
      | email address       |
      | first and last name |
      | user id             |

  @negative
  Scenario: Searching for only a first name is invalid
    When I search for users with only a first name
    Then the request fails because it is invalid

  @negative
  Scenario: Searching for only a last name is invalid
    When I search for users with only a last name
    Then the request fails because it is invalid

  @negative
  Scenario Outline: Searching for users with empty attributes is invalid
    When I search for users with empty <attributes>
    Then the request fails because it is invalid

    Examples:
      | attributes          |
      | email address       |
      | first and last name |
      | user id             |
