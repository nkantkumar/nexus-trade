@team-reference-data @domain-reference-data @contract:reference-rest
Feature: Symbol metadata resolution

  Scenario: Canonical symbol maps to exchange venue metadata
    When I request reference metadata for symbol AAPL
    Then symbol AAPL resolves to exchange NASDAQ
