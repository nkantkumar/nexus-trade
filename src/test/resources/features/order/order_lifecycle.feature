@team-order-management @domain-order @contract:order-api
Feature: Order lifecycle persistence

  Orders created through the service layer must be persisted and observable downstream via Kafka-capable flows.

  Scenario: Create order persists to the repository
    Given the order book has no persisted orders
    When I create an order for symbol AAPL quantity 100 limit price "150.00"
    Then a persisted order exists for symbol AAPL
