@reporting
Feature: Report user's client details
  As a member of the marketing team
  I want to able to report user registrations
  So that I can tell how we well we're doing at acquiring and retaining customers

  Background:
    Given I have registered an account

  Scenario: Registering a client with all details
    When I provide the client registration details:
      | name  | Test Device |
      | brand | Test Brand  |
      | model | Test Model  |
      | OS    | Test OS     |
    And I submit the client registration request
    Then a client registration message is sent
    And it contains the client's registration details

  Scenario: Updating all client information
    Given I have registered a client
    When I change my client's details to:
      | name  | Updated Device |
      | brand | Updated Brand  |
      | model | Updated Model  |
      | OS    | Updated OS     |
    And I request my client's information be updated
    Then a client update message is sent
    And it contains the client's update details

  Scenario: Deregistering a client
    Given I have registered a client
    When I request that my current client be deregistered
    Then a client deregistration message is sent
    And it contains the client's deregistration details
