@reporting
Feature: Report user details
  As a member of the marketing team
  I want to able to report user registrations
  So that I can tell how we well we're doing at acquiring and retaining customers

  Scenario: Registering a user
    When I provide valid registration details
    And I submit the registration request
    Then a user registration message is sent
    And it contains the user's registration details

  Scenario: Updating all user information
    Given I have registered an account
    When I change my first name to "Bob"
    And I change my last name to "Smith"
    And I change my email address
    And I change whether I allow marketing communications
    And I request my user information be updated
    Then a user update message is sent
    And it contains the user's update details
