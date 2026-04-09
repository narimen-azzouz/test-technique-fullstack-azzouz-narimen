# Backend Implementation Notes (Tendanz Pricing Engine)

This document explains what was implemented in the backend and why each technical choice was made.

## 1) Pricing calculation (`PricingService`)

### What was implemented
- `calculateQuote(QuoteRequest request)` in `PricingService` now:
  - Loads `Product`, `Zone`, and `PricingRule` from the database
  - Determines `AgeCategory` using `AgeCategory.fromAge(clientAge)`
  - Computes:

  $$\text{finalPrice} = \text{baseRate} \times \text{ageFactor} \times \text{zoneCoefficient}$$

  - Persists a `Quote` containing the computed prices and a JSON string of applied rules
  - Returns a `QuoteResponse`

### Why these choices
- **`BigDecimal` for money**: avoids rounding errors of floating point (`double`).
- **Rounding**: `setScale(2, RoundingMode.HALF_UP)` matches typical financial rounding and keeps responses stable.
- **Single responsibility**: the controller stays thin; pricing rules live in the service.
- **Applied rules persisted as JSON**: keeps the DB schema simple while preserving an audit trail of how the price was computed.

### Notes
- The response model used by this repository is the simplified backend DTO (`QuoteResponse`) and matches the frontend models in `frontend/src/app/models/*`.

## 2) Repository filtering (`QuoteRepository`)

### What was implemented
Added Spring Data JPA query methods:
- `findByClientName(String clientName)`
- `findByProductId(Long productId)`
- `findQuotesAboveThreshold(BigDecimal minPrice)` using JPQL with ordering
- `findByProductIdAndFinalPriceGreaterThanEqualOrderByFinalPriceDesc(Long productId, BigDecimal minPrice)` for combined filters

### Why these choices
- **Derived queries where possible**: less code, type-safe, and consistent with Spring Data conventions.
- **JPQL for “min price + ordering”**: expresses the requirement precisely (threshold + order by desc) and is easy to read.
- **Dedicated combined query**: avoids filtering in memory and keeps semantics correct at the database level.

## 3) Quote API (`QuoteController`)

### What was implemented
- `POST /api/quotes` creates a quote and returns `201 Created`.
- `GET /api/quotes/{id}` was already implemented and left as-is.
- `GET /api/quotes` now supports optional filtering:
  - `productId`
  - `minPrice`
  - and combined filtering when both are provided.

### Why these choices
- **HTTP semantics**: `201 Created` for creating resources.
- **Thin controller**: controller delegates pricing to the service and uses repository queries for listing.
- **Mapping without re-fetching**: for list results, the controller maps `Quote` → `QuoteResponse` through the service to avoid an extra DB call per row.

## 4) Error handling (`GlobalExceptionHandler`)

### What was implemented
- Validation errors (`MethodArgumentNotValidException`) return `400` with an `errors` map: field → message.
- Domain not-found / invalid references (`IllegalArgumentException`) return `404` with message.
- Fallback handler (`Exception`) returns `500` with a generic message (no internal details).
- Added a specific `NoResourceFoundException` handler to keep missing static resources (e.g. favicon) as `404`.

### Why these choices
- **Consistent JSON error format** makes frontend consumption straightforward.
- **Do not leak internal stack traces**: important for production-grade APIs.
- **Structured validation output**: better UX than a single message string.

## 5) JSON and Java time serialization (`PricingApplication`)

### What was implemented
- The `ObjectMapper` bean is built using Spring’s `Jackson2ObjectMapperBuilder`.

### Why this choice
- A raw `new ObjectMapper()` does **not** automatically register modules like Java Time (JSR-310), which breaks serialization of `LocalDateTime` fields in entities/DTOs.
- Using Spring’s builder aligns with Spring Boot defaults and ensures consistent JSON behavior across the application.

## 6) CORS for Angular dev (`WebConfig`)

### What was implemented
- Allow CORS for `/api/**` from `http://localhost:4200`.

### Why this choice
- During development, Angular runs on a different origin than the backend. Without CORS, browser calls would fail even when the API works.
- Scope is limited to `/api/**` to avoid over-exposing non-API endpoints.

## 7) Known non-backend items (to handle later)

- Root `README.md` contains unresolved merge conflict markers. This should be fixed before final submission.
- Frontend TODOs and backend unit test TODOs are intentionally left for subsequent steps, as planned.
