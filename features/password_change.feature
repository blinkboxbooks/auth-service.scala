Feature: Password Change
As a registered user
I want to be able to change my password from time to time
So that my account will be more difficult to break into

  Scenario: when authenticated, change password with correct existing password and new password that passes validation
    Given I have registered an account
    And I am authenticated
    And I create a request to change my password
    And the request includes my desired new password
    And the desired new password passes validation
    And the request contains my correct existing password
    When the request is submitted
    Then the password for the account is changed
    And I am able to use my new password for all subsequent authentication attempts

  Scenario: when authenticated, change password with correct existing password and new password that fails validation
    Given I have registered an account
    And I am authenticated
    And I create a request to change my password
    And the request contains my correct existing password
    And the request includes my desired new password
    And the desired new password fails validation
    When the request is submitted
    Then the password for the account remains the same
    And an error is returned

  Scenario: when authenticated, change password with incorrect password
    Given I have registered an account
    And I am authenticated
    And I create a request to change my password
    And the request includes my desired new password
    And the request does not contain my existing password
    When the request is submitted
    Then the password for the account remains the same
    And an error is returned

  Scenario: when authenticated, change password with no new password specified
    Given I have registered an account
    And I am authenticated
    And I create a request to change my password
    And the request does not include a new password
    When the request is submitted
    Then the password for the account remains the same
    And an error is returned

  Scenario: when not authenticated, change password
    Given I am not authenticated
    And I create a request to change my password
    When the request is submitted
    Then an error is returned