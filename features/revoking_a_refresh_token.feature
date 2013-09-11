@revocation @refresh_tokens
Feature: Revoking a refresh token
  As a user
  I want to be able to revoke a refresh token
  So that I can stop clients from acting on my behalf

  Background:
    Given I have registered an account

  Scenario: Revoking a refresh token
    When I request that my refresh token be revoked
    Then the request succeeds

  Scenario: Revoking a refresh token without authorisation
  Users don't need to be authenticated to revoke a refresh token. A refresh token can be
  exchanged for an access token anyway so there would be no security in requiring it.

    When I request that my refresh token be revoked, without my access token
    Then the request succeeds

  Scenario: Trying to revoke a nonexistent refresh token
  There are no security implications around telling a user the token wasn't valid; this
  information can be found when trying to use it to refresh an access token anyway, and
  that action would be a lot more useful to any potential attacker.

    When I request that a nonexistent refresh token be revoked
    Then the response indicates that my refresh token is invalid