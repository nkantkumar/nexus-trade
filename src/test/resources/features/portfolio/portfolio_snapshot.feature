@team-portfolio @domain-portfolio @contract:portfolio-rest
Feature: Portfolio snapshot reflects execution-driven holdings

  Scenario: Holdings exposed after synthetic execution feed
    Given account ACC1 executed a fill for 100 AAPL shares at price 150.5
    When I request the portfolio snapshot for symbol AAPL
    Then the snapshot quantity for symbol AAPL is 100
