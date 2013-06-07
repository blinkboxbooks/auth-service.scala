@registration
Feature: Registration
  As a user
  I want to be able to register an account
  So that I can use services that require my identity

  Background:
    Given that the auth server is at "http://localhost:9393/"
  
  Scenario: Registering with all the required information
    Given I have provided the details required to register
    When I submit the registration request
    Then the response contains an access token and a refresh token

  Scenario Outline: Registering with missing details
    Given I have provided the details required to register
    But the <required detail> is missing
    When I submit the registration request
    Then the response contains an error of type "invalid_request"

    Examples:
      | required detail |
      | first_name      |
      | last_name       |
      | username        |
      | password        |

  Scenario Outline: Registering with an invalid email address
    Given I have provided the details required to register
    But the email address has the value <invalid value>
    When I submit the registration request
    Then the response contains an error of type "invalid_request"

    Examples: Malformed addresses should not be accepted
      | invalid value    |
      | no.at.symbol.com |
      | no@dots          |

  Scenario Outline: Registering with an invalid password
    Given I have provided the details required to register
    But the password has the value <invalid value>
    When I submit the registration request
    Then the response contains an error of type "invalid_request"

    Examples: Passwords under six characters are not allowed
      | invalid value    |
      | abcd             |
      | aA9!w            |

  # Scenario: Registering with an invalid email address
  #   Given I have provided the details required to register
  #   But the email address is invalid
  #   When I submit the registration request
  #   Then the response contains an error of type "invalid_request"
