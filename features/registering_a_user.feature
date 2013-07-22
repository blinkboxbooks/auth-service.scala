@users @user_registration
Feature: Registration
  As a user
  I want to be able to register an account
  So that I can use services that require my identity
  
  Scenario: Registering with all the required information
    Given I have provided valid registration details
    When I submit the registration request
    Then the response contains an access token and a refresh token

  Scenario: Trying to register without accepting the terms and conditions
    Given I have provided valid registration details
    But I have not accepted the terms and conditions
    When I submit the registration request
    Then the response indicates that the request was invalid

  Scenario: Trying to register with an email address that is already registered
    Given I have registered an account
    When I provide the same registration details I previously registered with
    And I submit the registration request
    Then the response indicates that the request was invalid
    And the reason is that the email address is already taken

  Scenario Outline: Trying to register with missing details
    Given I have provided valid registration details, except <detail> which is missing
    When I submit the registration request
    Then the response indicates that the request was invalid

    Examples: Required details
      These details are required for registration
      | detail                         |
      | first name                     |
      | last name                      |
      | email address                  |
      | password                       |
      | accepted terms and conditions  |
      | allow marketing communications |

  Scenario Outline: Trying to register with invalid details
    Given I have provided valid registration details, except <detail> which is "<value>"
    When I submit the registration request
    Then the response indicates that the request was invalid

    Examples: Malformed email address
      The email address must have one @ symbol with a . after it and characters at each end and in between 
      | detail        | value             |
      | email address | user.example.org  |
      | email address | user@example      |
      | email address | user.example@com  |
      | email address | user@@example.org |
      | email address | user@example.     |
      | email address | @example.org      |

    Examples: Password too short
      The password must be at least six characters in length
      | detail   | value |
      | password | aY9!w |

    Examples: Name too long
      The first name and/or last name can't be more than fifty characters
      | detail     | value                                                |
      | first name | abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz |
      | last name  | abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz |