Feature: Password Change
As a registered user
I want to be able to change my password from time to time
So that my account will be more difficult to break into

  Background:
    Given I have registered an account

  Scenario: when authenticated, change password with correct existing password and new password that passes validation
    When I provide valid password change details
    And I request my password be changed
    Then I am able to use my new password to authenticate
    And I am not able to use my old password to authenticate

  Scenario: when authenticated, change password  to same as existing password
    When I provide valid password change details
    But my new password is the same as my current password
    And I request my password be changed
    Then the request fails because it is invalid
    And I am still able to use my old password to authenticate

  Scenario: when authenticated, change password with correct existing password and new password that fails validation
    When I provide valid password change details
    But my new password is too short
    And I request my password be changed
    Then the request fails because it is invalid
    And the reason is my provided new password is invalid
    And I am still able to use my old password to authenticate


  Scenario: when authenticated, change password with incorrect password
    When I provide valid password change details
    But I provide a wrong password as my current password
    And I request my password be changed
    Then the request fails because it is invalid
    And the reason is my provided old password is wrong
    And I am still able to use my old password to authenticate

  Scenario: when authenticated, change password with no new password specified
    When I provide valid password change details
    But I do not provide a new password
    And I request my password be changed
    Then the request fails because it is invalid
    And the reason is my provided new password is invalid
    Then I am able to use my old password to authenticate
    And I am not able to use my new password to authenticate

  Scenario: when not authenticated, change password
    When I request my password be changed, without my access token
    Then the request fails because I am unauthorised
    And I am still able to use my old password to authenticate
