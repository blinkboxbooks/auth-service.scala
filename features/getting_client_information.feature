@clients @client_info
Feature: Getting client information
  As a client
  I want to be able to get information about myself
  So that I can make sure the server is up to date

  Background:
    Given I have registered an account
    And I have registered a client

  Scenario: Getting client information
    Given I have provided my access token
    When I submit the client information request
    Then the response contains client information, excluding the client secret
    And the response is not cacheable

  Scenario: Trying to get client information without authorisation
    Given I have not provided my access token
    When I submit the client information request
    Then the request fails because I am unauthorised


