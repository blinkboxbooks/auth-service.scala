@reporting
Feature: Report user's client details
  As a member of the marketing team
  I want to able to report user registrations
  So that I can tell how active customers are with our service

  Background:
    Given a user has registered an account

  Scenario: A user registers a client
    When they submit the client registration request
    Then a client registration message is sent
    And it contains the client's details:
      | user id                       |
      | client registration timestamp |
      | device: id                    |
      | device: name                  |
      | device: brand                 |
      | device: model                 |
      | device: os                    |

  Scenario Outline: A user update their client's information
    Given a user has registered a client
    When they change their client's <changeable_detail> to <new_value>
    And they request their client's information be updated
    Then a client update message is sent
    And it contains the client's old details
    And it contains the client's new details

    Examples: Details which can be changed for a client
      | changeable_detail | new_value      |
      | name              | Updated Device |
      | brand             | Updated Brand  |
      | model             | Updated Model  |
      | os                | Updated OS     |

  Scenario: A user deregisters their client
    Given a user has registered a client
    When they request that their current client be deregistered
    Then a client deregistration message is sent
    And it contains the client's deregistration details
