@reporting
Feature: Report user details
  As a member of the marketing team
  I want to able to report user registrations
  So that I can tell how well we're doing at acquiring customers

  Scenario: A user registers
    When a user provides valid registration details
    And they submit the registration request
    Then a user registration message is sent
    And it contains the user's details:
      | user: id                                  |
      | user: username                            |
      | user: first name                          |
      | user: last name                           |
      | user: marketing communications preference |
    And it contains a valid ISO-8601 user event timestamp

  Scenario Outline: A user updates their details
    Given a user has registered an account
    When they change their <changeable_detail> to <new_value>
    And they request their user information be updated
    Then a user update message is sent
    And it contains the user's id
    And it contains the user's old details:
      | user: username                            |
      | user: first name                          |
      | user: last name                           |
      | user: marketing communications preference |
    And it contains the user's new details:
      | user: username                            |
      | user: first name                          |
      | user: last name                           |
      | user: marketing communications preference |
    And it contains a valid ISO-8601 user event timestamp

    Examples: Fields which can be changed
      | changeable_detail                   | new_value |
      | first name                          | Bob       |
      | last name                           | Smith     |
      | username                            | any email |
      | marketing communications preference | none      |
