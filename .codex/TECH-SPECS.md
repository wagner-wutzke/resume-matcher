# Technical Specification & Coding Standards

## 1. Purpose & Scope

This document defines the immutable technical decisions, libraries, and formatting rules for the
codebase. All feature implementations and AI-generated code must strictly adhere to these standards.

## 2. Tech Stack & Core Dependencies

### Technical Stack
* **Language:** Java 21 (LTS)
* **Build Tool:** Maven 3.9+ 
* **Framework:** Spring Boot 4.1, Spring Boot AI, Spring Boot WEB MVC, Spring Data JPA
* **Logging Facade:** SLF4J (`org.slf4j.Logger`) backed by Logback
* **Vector Store:** Qdrant
* **NoSQL Database:** Mongo DB
* **Webapp:** vanilla JavaScript, CSS, Bootstrap and pure HTML.
* **Testing:** JUnit 5 and Mockito

## 3. Code Formatting & Style

* **Standard:** Follow the [Google Java Style Guide](https://github.io).
* **Indentation:** 2 spaces (no tabs).
* **Line Length:** Maximum 100 characters per line.
* **Imports:**
    * No wildcard imports (`import java.util.*;` is forbidden).
    * Group imports in order: `java.*`, `javax.*`, third-party libraries, project-internal packages.
* **Enforcement:** Code must pass `spotless:check` (configured with Google Java Format) before
  committing.

## 4. Logging Standards (SLF4J)

* **Logger Instantiation:** Always declare loggers as `private static final` using SLF4J directly (
  or Lombok's `@Slf4j` if enabled in the project).
  ```java
  private static final Logger log = LoggerFactory.getLogger(MyClass.class);
  ```
* **Forbidden Loggers:** Never use `System.out.println()`, `e.printStackTrace()`, or
  `java.util.logging`.
* **Parameterized Messages:** Always use `{}` placeholders instead of string concatenation to avoid
  unnecessary string allocation.
    * *Good:* `log.info("Processing order {} for user {}", orderId, userId);`
    * *Bad:* `log.info("Processing order " + orderId);`
* **Log Levels:**
    * `ERROR`: Unrecoverable failures requiring immediate attention (always pass the `Throwable` as
      the final argument).
    * `WARN`: Recoverable anomalies or degraded states.
    * `INFO`: High-level lifecycle events and key business milestones.
    * `DEBUG`: Detailed diagnostic flow (must not log sensitive PII/credentials).

## 5. Error Handling & Testing

* **Exceptions:** Prefer custom domain exceptions extending `RuntimeException` over checked
  exceptions.
* **Unit Tests:** JUnit 5 + Mockito; follow the `given_when_then` or
  `shouldDoSomething_whenCondition` naming convention.
