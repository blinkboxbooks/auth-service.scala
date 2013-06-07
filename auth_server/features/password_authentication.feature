@authentication
Feature: Password authentication
  As a user
  I want to be able to authenticate with my email address and password
  So that I can use services that require my identity

  Background:
    Given that the auth server is at "http://localhost:9393/"
    And I have registered an account

  