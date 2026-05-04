# Behavior-driven development (BDD) across teams

This repository uses [Cucumber](https://cucumber.io/) with Spring so **product, compliance, and engineering** can share executable specifications while each squad owns its slices.

## Layout

| Path | Ownership |
|------|-----------|
| `src/test/resources/features/<domain>/` | Domain teams write Gherkin; platform keeps naming consistent |
| `src/test/java/com/trading/bdd/steps/<domain>/` | Step definitions wiring Spring beans (thin adapters) |
| `src/test/java/com/trading/bdd/config/` | Shared Cucumber + Spring bootstrapping |
| `src/test/java/com/trading/bdd/support/` | Test-only infrastructure (e.g. Redis stand-ins) |

## Tag taxonomy

Use tags so CI can **shard** work by squad or concern:

- **`@team-<name>`** — owning squad (e.g. `@team-order-management`).
- **`@domain-<name>`** — bounded context (mirrors packages under `com.trading.*`).
- **`@contract:<id>`** — stable identifier for consumer-driven contracts or API catalogs.

Exclude unfinished work with `@wip` (filtered out of default runs).

## Running locally

Docker must be available so Testcontainers can start Redis for the Spring context (`StringRedisTemplate` cannot be Mockito-mocked reliably).

```bash
mvn verify
```

The Cucumber suite runs in the **Failsafe** phase (`*IT.java`). Unit-style tests should stay in Surefire (`*Test.java`).

## Scaling CI / cross-team throughput

1. **Shard by tag** — run separate jobs: `-Dcucumber.filter.tags="@team-risk"`, etc.
2. **Parallelism** — turn on `cucumber.execution.parallel.enabled` in `junit-platform.properties` once scenarios do not share JVM-global singletons (Kafka, Chronicle paths, static counters).
3. **Living documentation** — publish `target/cucumber-reports/` as a build artifact for stakeholders.

## Principles

- Features describe **behavior**, not implementation (avoid UI selectors and internal class names in scenarios).
- Steps stay **thin**: call application services or HTTP boundaries; avoid duplicating domain rules in glue code.
- Prefer **one domain folder per bounded context** so merges rarely conflict across squads.
