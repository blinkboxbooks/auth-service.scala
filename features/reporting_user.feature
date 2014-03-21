@reporting @in_proc_only
Feature: Report user details
  As a member of the marketing team
  I want to able to report user registrations
  So that I can tell how well we're doing at acquiring customers

  @smoke
  Scenario: A user registers
    When a user provides valid registration details
    And they submit the registration request
    Then a user registration message is sent
    And it contains the user's details:
      | id                             |
      | username                       |
      | first name                     |
      | last name                      |
      | allow marketing communications |
    And it contains a user event timestamp

  Scenario: A user registers with their device
    When a user provides valid registration details
    And they provide the client registration details:
      | name  | Test Device |
      | brand | Test Brand  |
      | model | Test Model  |
      | OS    | Test OS     |
    And they submit the user and client registration request
    Then a client registration message is sent
    And it contains the user's id
    And it contains the client's details:
      | id    |
      | name  |
      | brand |
      | model |
      | os    |
    And it contains a client event timestamp
    And a user registration message is sent
    And it contains the user's details:
      | id                             |
      | username                       |
      | first name                     |
      | last name                      |
      | allow marketing communications |
    And it contains a user event timestamp

  Scenario Outline: A user updates their details
    Given a user has registered an account
    When they change their <changeable_detail> to <new_value>
    And they request their user information be updated
    Then a user update message is sent
    And it contains the user's id
    And it contains the user's old details:
      | username                       |
      | first name                     |
      | last name                      |
      | allow marketing communications |
    And it contains the user's new details:
      | username                       |
      | first name                     |
      | last name                      |
      | allow marketing communications |
    And it contains a user event timestamp

    Examples: Fields which can be changed
      | changeable_detail              | new_value |
      | first name                     | Bob       |
      | last name                      | Smith     |
      | username                       | any email |
      | allow marketing communications | false     |
