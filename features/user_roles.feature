@users @roles
Feature: User roles
  As a system administrator
  I want to be able to control which roles users are in
  So I can restrict access to various services using role-based security

  Scenario: Newly registered users have no roles
    Given I have registered an account
    When I request information about the access token
    Then the response contains access token information
    And it does not contain any role information

  Scenario: Users can be in multiple roles
    Given I am authenticated as a user in the "employee" and "content manager" roles
    When I request information about the access token
    Then the response contains access token information
    And it indicates that I am in the "employee" role
    And it indicates that I am in the "content manager" role