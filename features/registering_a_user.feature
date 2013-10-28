@users @registration @user_registration
Feature: Registration
  As a user
  I want to be able to register an account
  So that I can use services that require my identity

  As a UK-based company
  I want to prevent customers outside the UK from registering
  So that I can manage my expansion to other countries

  Scenario: Registering with all the required information
    When I provide valid registration details
    And I submit the registration request
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And it is not cacheable
    Then I receive a welcome email

  Scenario: Registering a client with the user registration
    When I provide valid user registration details
    And I provide the client registration details:
      | name | Test Device |
      | brand | Test Brand |
      | model | Test Model |
      | OS | Test OS |
    And I submit the user and client registration request
    Then the response contains client information, including a client secret
    And it contains basic user information matching my details
    And the client details match the provided details
    And I receive a welcome email
    And the response contains an access token and a refresh token

  Scenario Outline: Trying to register client and user simultaneously with missing user details
    When I provide valid registration details, except <user_registration_detail> which is missing
    And I provide a client model
    And I submit the user and client registration request
    Then the request fails because it is invalid
    And no email is sent

  Examples: Required details
  These details are required for registration
    | user_registration_detail   |
    | first name                     |
    | last name                      |
    | email address                  |
    | password                       |
    | accepted terms and conditions  |
    | allow marketing communications |
    | client name                    |
    | client brand                   |
    | client model                   |
    | client os                      |

  Scenario Outline: Trying to register client and user simultaneously with missing client details
    When I provide valid registration details
    And I do not provide a client <detail>
    And I submit the user and client registration request
    Then the request fails because it is invalid
    And no email is sent

  Examples:
    | detail |
    | name   |
    | brand  |
    | model  |
    | OS     |


  Scenario: Registering with a name containing international characters
    When I provide valid registration details
    And my first name is "Iñtërnâtiônàlizætiøn"
    And my last name is "中国扬声器可以阅读本"
    And I submit the registration request
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And it is not cacheable
    And I receive a welcome email

  Scenario: Registering without allowing marketing communications
    When I provide valid registration details
    And I have not allowed marketing communications
    And I submit the registration request
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And it is not cacheable
    And I receive a welcome email

  Scenario: Trying to register without accepting the terms and conditions
    When I provide valid registration details
    But I have not accepted the terms and conditions
    And I submit the registration request
    Then the request fails because it is invalid
    And no email is sent

  Scenario: Trying to register with an email address that is already registered
    Given I have registered an account
    When I provide the same registration details I previously registered with
    And I submit the registration request
    Then the request fails because it is invalid
    And the reason is that the email address is already taken
    And no email is sent

  Scenario Outline: Trying to register with missing details
    When I provide valid registration details, except <detail> which is missing
    And I submit the registration request
    Then the request fails because it is invalid
    And no email is sent

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
    When I provide valid registration details, except <detail> which is "<value>"
    And I submit the registration request
    Then the request fails because it is invalid
    And no email is sent

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

  Scenario Outline: Registering while geolocated in permitted countries
    Given my IP address would geolocate me in <country>
    When I try to register an account
    Then the request succeeds

    Examples:
      | country |
      | GB      |
      | IE      |

  Scenario Outline: Registering while on the local machine or a private network
    For development and testing purposes internally, we need to ensure that we do not prevent
    registration from the local machine or private network addresses which cannot be geolocated

    Given my IP address is in the range <range>
    When I try to register an account
    Then the request succeeds

    Examples:
      | range          |
      | 127.0.0.1      |
      | 192.168.0.0/16 |
      | 172.16.0.0/12  |
      | 10.0.0.0/8     |

  Scenario Outline: Trying to register while geolocated outside permitted countries
    Given my IP address would geolocate me in <country>
    When I try to register an account
    Then the request fails because it is invalid
    And the reason is that my country is geoblocked
    And no email is sent

    Examples:
      | country |
      | FR      |
      | US      |

  Scenario: Trying to register when geolocation cannot be determined
    Given my IP address cannot be geolocated
    When I try to register an account
    Then the request fails because it is invalid
    And the reason is that my country is geoblocked
    And no email is sent