@team-risk @domain-risk @contract:risk-pipeline
Feature: Pre-trade risk pipeline outcome

  Scenario: Well-sized trade passes aggregate checks
    When I run a pre-trade risk check for account ACC1 symbol AAPL quantity 100 price 150.0 vwap 150.0 margin 100000.0
    Then the risk outcome is approved

  Scenario: Position limit rejects oversized intent
    When I run a pre-trade risk check for account ACC1 symbol AAPL quantity 2000001 price 10.0 vwap 10.0 margin 100000000.0
    Then the risk outcome is rejected
