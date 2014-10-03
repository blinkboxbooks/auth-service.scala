@authentication @passwords @sso
Feature: Books users can authenticate into SSO and vice versa
  As a blinkbox user that uses movies, music and books services
  I want to be able to authenticate with my email address through any of the services
  So that I can log into all blinkbox services with one account

  Scenario Outline: Authenticating to books with existing SSO accounts
    Given I have an SSO account registered by <service>
    When I provide my email address and password
    And I authenticate onto the books website
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And my details are correct in the SSO system

  Examples:
    | service      |
    | movies       |
    | music        |
    | books        |

  Scenario Outline: Authenticating to books with existing linked SSO accounts
    Given I have an SSO account registered by <service>
    And my SSO account is linked to the service I registered with
    When I provide my email address and password
    And I authenticate onto the books website
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And my details are correct in the SSO system

  Examples:
    | service      |
    | movies       |
    | music        |

  Scenario Outline: Creating a books account and then authenticating into other services via SSO
    Given I have registered an account
    When I provide my email address and password
    And I submit an authentication request to SSO from <service>
    Then the response contains an access token and a refresh token

  Examples:
    | service |
    | movies  |
    | music   |
    | books   |
