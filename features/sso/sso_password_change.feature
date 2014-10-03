@authentication @passwords @sso
Feature: Changing a password using books is reflected in sso and vice versa
  As a blinkbox user that uses movies, music and books services
  I want to be able to change my password
  So that I can log into all blinkbox services with my new password

  Scenario Outline: Changing your password in books is reflected in SSO
    Given I have registered an account
    When I provide valid password change details
    And I request my password be changed
    Then I am able to use my new password to authenticate to SSO via <service>
    And I am not able to use my old password to authenticate to SSO via <service>

  Examples:
    | service |
    | movies  |
    | music   |
    | books   |

  Scenario Outline: Changing your password in SSO is reflected in the books service
    Given I have an SSO account registered by <service>
    And I provide valid password change details
    When I update my password in SSO
    And I am able to use my new password to authenticate
    And I am not able to use my old password to authenticate

  Examples:
    | service |
    | movies  |
    | music   |
    | books   |
