Identity Service — Full Scaffold
Generate a production Java 21 Spring Boot 3 microservice for an Identity Service in a trading platform. Include: JWT issuance and validation using Spring Security OAuth2, MFA via TOTP (Google Authenticator compatible), API key generation and hashing (SHA-256), session management with Redis (Lettuce), refresh token rotation, rate limiting on login endpoints. Package: com.trading.identity. Use records for DTOs, sealed interfaces for auth results. Include all imports and annotations.

Order Management — Service Layer + State Machine
Generate a production Java 21 Spring Boot 3 Order Management service for a trading platform. Implement a state machine for order lifecycle: NEW → PENDING_ACK → PARTIALLY_FILLED → FILLED / CANCELLED / REJECTED. Include: order creation, modification, cancellation, retry logic with exponential backoff, Kafka producer publishing OrderEvent to topic "order-events", Redis caching for hot orders, JPA entities with optimistic locking. Package: com.trading.order. Use records for commands and events.

Market Data Streaming — Kafka Consumer + Order Book
Generate a production Java 21 trading Market Data service. Include: Kafka consumer ingesting raw tick data from multiple feeds (equities, forex, crypto), feed normalization into a canonical MarketDataEvent record, L2 order book reconstruction using a TreeMap<BigDecimal, Long> for bids/asks, symbol mapping via Reference Data Service client, fan-out to downstream consumers via Kafka producer. Use Chronicle Queue for low-latency local persistence. Package: com.trading.marketdata.

Risk Engine — Pre-trade Check Pipeline
Generate a production Java 21 risk engine for a trading platform. Implement a pre-trade risk check pipeline with: position limit checks (per symbol, per account), notional exposure limits, fat finger detection (price vs VWAP deviation > threshold), margin sufficiency check, credit limit validation. Use Chain of Responsibility pattern. Each check returns a sealed RiskResult (Approved / Rejected(reason) / Breach(severity)). Redis for real-time position cache. Aeron IPC for ultra-low latency check invocation. Package: com.trading.risk.

Matching Engine — Lock-free Order Book
Generate a production Java 21 matching engine for an exchange. Requirements: lock-free order book using AtomicReference and CAS operations, price-time priority FIFO matching, support for limit and market orders, CPU affinity hints via thread naming, Chronicle Queue for order ingestion and trade output, sub-microsecond target latency (avoid allocations — use object pools and primitive arrays), Aeron for network transport. Produce TradeEvent on match. Package: com.trading.matching. Add comments explaining lock-free design decisions.

Trade Execution — Smart Order Router
Generate a production Java 21 Smart Order Router (SOR) for a trading platform. Include: venue registry with latency and fill-rate scores, order slicing logic (TWAP/VWAP split), best execution algorithm selecting venue by price + latency score, QuickFIX/J integration for FIX 4.4 order routing to multiple venues, execution report aggregation, partial fill handling, Kafka producer publishing ExecutionReport events. Package: com.trading.execution.

Compliance — Trade Surveillance
Generate a production Java 21 compliance service for a trading platform. Include: Kafka consumer on "order-events" and "execution-events" topics, spoofing detection (large order placed then cancelled before fill > 80%), wash trading detection (same account on both sides within time window), AML transaction monitoring with threshold alerts, OFAC sanctions screening via external REST client with circuit breaker (Resilience4j), immutable audit log written to Chronicle Queue. Package: com.trading.compliance.

Settlement — FIX + ISO 20022
Generate a production Java 21 settlement and clearing service for a trading platform. Include: post-trade settlement workflow triggered by ExecutionReport, DVP (Delivery vs Payment) state machine, reconciliation job comparing internal records vs custodian feed, SWIFT ISO 20022 message generation (pacs.008 for credit transfer), QuickFIX/J for FIX Execution Report parsing, fund movement instruction publishing to Kafka topic "settlement-instructions". Package: com.trading.settlement.

Portfolio — PnL + Risk Metrics
Generate a production Java 21 portfolio management service for a trading platform. Include: real-time position tracking updated via Kafka consumer on "execution-events", realized and unrealized PnL calculation using FIFO cost basis, Value at Risk (VaR) calculation using historical simulation (95% confidence, 1-day horizon), Sharpe ratio computation, max drawdown tracking, Redis for live position cache, REST API exposing portfolio snapshot. Package: com.trading.portfolio.

Pricing Engine — Options Greeks
Generate a production Java 21 pricing engine for a trading platform. Include: Black-Scholes implementation for European options pricing, Greeks calculation (Delta, Gamma, Theta, Vega, Rho), Monte Carlo simulation for exotic options (configurable paths), mark-to-market job running on schedule, NAV calculation for fund portfolios, volatility surface interpolation (bilinear). Use virtual threads (Project Loom) for Monte Carlo parallelism. Package: com.trading.pricing.

Reporting — Immutable Audit + Replay
Generate a production Java 21 reporting service for a trading platform. Include: event sourcing pattern with immutable audit log stored in Chronicle Queue, Kafka consumer capturing all domain events (orders, executions, risk breaches), regulatory report generation (MiFID II transaction report as CSV), trade history query API with pagination, event replay mechanism to rebuild state from a given timestamp. Package: com.trading.reporting.

Strategy / Algo Engine
Generate a production Java 21 algorithmic trading strategy engine. Include: abstract Strategy base class with onMarketData(MarketDataEvent), onExecution(ExecutionEvent), lifecycle hooks (start/stop/pause), built-in mean reversion strategy implementation using Bollinger Bands, position sizing with Kelly Criterion, signal publishing to Kafka topic "algo-signals", Chronicle Queue for strategy state persistence, Micrometer metrics (signal count, PnL, fill rate). Package: com.trading.strategy.