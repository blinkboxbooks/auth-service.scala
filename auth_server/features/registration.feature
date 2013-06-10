@registration
Feature: Registration
  As a user
  I want to be able to register an account
  So that I can use services that require my identity

  Background:
    Given that the auth server is at "http://localhost:9393/"
  
  Scenario: Registering with all the required information
    Given I have provided valid registration details
    When I submit the registration request
    Then the response contains an access token and a refresh token

  Scenario: Trying to register with an email address that is already registered
    Given I have registered an account
    When I provide the same registration details I previously registered with
    And I submit the registration request
    Then the response indicates that the request was invalid

  Scenario Outline: Trying to register with missing details
    Given I have provided valid registration details, except <detail> which is missing
    When I submit the registration request
    Then the response indicates that the request was invalid

    Examples: Required details
      These details are required for registration
      | detail        |
      | first name    |
      | last name     |
      | email address |
      | password      |

  Scenario Outline: Trying to register with invalid details
    Given I have provided valid registration details, except <detail> which is "<value>"
    When I submit the registration request
    Then the response indicates that the request was invalid

    Examples: Malformed email address
      The email address must have at least an @ symbol with a . after it      
      | detail        | value        |
      | email address | no.at.symbol |
      | email address | no@dots      |

    Examples: Password too short
      The password must be at least six characters in length
      | detail   | value |
      | password | aY9!w |