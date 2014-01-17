@dev
Feature: Search for users
  As a customer services representative
  I want to be able to find the users who have problems
  So that I can see information about them

  Background:
    Given I am authenticated as a user in the "customer services representative" role
    And there is a registered user with the attributes:
      | attribute  | type   | value                                  | description       |
      | First Name | String | Jacinta-123                            | The first name    |
      | Last Name  | String | Pennington-666                         | The last name     |
      | Username   | String | jacinta-123.pennington-666@bbbtest.com | The email address |
    And there is a registered user with the attributes:
      | attribute  | type   | value                                  | description       |
      | First Name | String | Balthazar-123                          | The first name    |
      | Last Name  | String | Spalding-666                           | The last name     |
      | Username   | String | balthazar-123.spalding-666@bbbtest.com | The email address |

  Scenario: Users can be found by username
    When I search for users with username "jacinta-123.pennington-666@bbbtest.com"
    Then the response contains a list containing one user
    And the first user has the following attributes:
      | attribute  | type   | value                                  | description       |
      | First Name | String | Jacinta-123                            | The first name    |
      | Last Name  | String | Pennington-666                         | The last name     |
      | Username   | String | jacinta-123.pennington-666@bbbtest.com | The email address |


