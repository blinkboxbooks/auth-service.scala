Feature: Role based security
  As a system administrator
  I want to be sure that only people in the correct roles can access administrative APIs
  So that the security of the system is maintained

  Scenario: Users with no roles cannot search for users
    Given I am authenticated as a user with no roles
    And there is a registered user, call her "Alice"
    When I search for users with Alice's email address
    Then the request fails because this is forbidden