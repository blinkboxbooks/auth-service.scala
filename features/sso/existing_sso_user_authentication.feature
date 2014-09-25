@authentication @passwords @sso
Feature: Password authentication with existing SSO accounts
  As an existing SSO user
  I want to be able to authenticate with my email address and password on the books website
  So that my books account is linked to my SSO account

  Scenario Outline: Authenticating to books with existing SSO accounts
    Given I have an SSO account registered by <account_type>
    When I provide my email address and password
    And I authenticate onto the books website
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And my details are correct in the SSO system

  Examples:
    | account_type |
    | movies       |
    | music        |
    | books        |

  Scenario Outline: Authenticating to books with existing linked SSO accounts
    Given I have an SSO account registered by <account_type>
    And my SSO account is linked to the service I registered with
    When I provide my email address and password
    And I authenticate onto the books website
    Then the response contains an access token and a refresh token
    And it contains basic user information matching my details
    And my details are correct in the SSO system

  Examples:
    | account_type |
    | movies       |
    | music        |
