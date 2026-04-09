# Frontend Implementation (Angular)

Date: 2026-04-09

This document explains how the Angular frontend TODOs were implemented, the key design choices, and how the UI interacts with the backend pricing engine.

## 1) Architecture & Data Flow

The frontend is a small Angular 17 app using **standalone components** and Angular’s built-in providers:

- Routing is configured in `src/app/app.routes.ts`
- HTTP is enabled via `provideHttpClient()` in `src/main.ts`
- API communication is isolated in two services:
  - `ProductService` for products
  - `QuoteService` for quotes

Pages:

- Quote list: `GET /api/quotes` (with optional filters)
- Quote form: `GET /api/products` (to populate dropdown) then `POST /api/quotes`
- Quote detail: `GET /api/quotes/:id`

## 2) Environment Configuration

The backend base URL is configured via `environment.apiUrl`.

- Dev: `src/environments/environment.ts` → `http://localhost:8080/api`
- Prod: `src/environments/environment.prod.ts` (placeholder)

All HTTP calls use `${environment.apiUrl}` + endpoint path.

## 3) Services

### 3.1 ProductService

File: `src/app/services/product.service.ts`

- `getProducts()` performs `GET {apiUrl}/products`
- Errors are handled with `catchError()` and converted into a user-friendly `Error`

### 3.2 QuoteService

File: `src/app/services/quote.service.ts`

Implemented methods:

- `createQuote(request)` → `POST {apiUrl}/quotes`
- `getQuote(id)` → `GET {apiUrl}/quotes/{id}`
- `getQuotes(filters?)` → `GET {apiUrl}/quotes` with optional `HttpParams`:
  - `productId`
  - `minPrice`

Error handling:

- Logs errors to console for debugging.
- Attempts to surface backend-provided messages via `error.error.message` when available.

## 4) Pages / UX Behavior

### 4.1 Quote Form

File: `src/app/pages/quote-form/quote-form.component.ts`

On init:

- Loads products from the API and populates the product dropdown.

On submit:

- Validates the form; if invalid, marks all fields as touched.
- Builds a `QuoteRequest`:
  - `clientName` (trimmed)
  - `productId` (number)
  - `zoneCode` (string)
  - `clientAge` (number)
- Calls `QuoteService.createQuote()`.
- On success navigates to the quote detail route: `/quotes/{quoteId}`.

### 4.2 Quote List

File: `src/app/pages/quote-list/quote-list.component.ts`

On init:

- Loads products for the filter dropdown.
- Loads quotes from the API.

Filtering:

- Builds a filter object from `selectedProductId` and `minPrice`.
- Calls `QuoteService.getQuotes(filters)` (server-side filtering).

Sorting:

- Done in-memory on the currently displayed list (`filteredQuotes`).
- Supports sorting by:
  - creation date (`createdAt`)
  - final price (`finalPrice`)

Navigation:

- Clicking a row navigates to `/quotes/:id`.

### 4.3 Quote Detail

File: `src/app/pages/quote-detail/quote-detail.component.ts`

- Reads `id` from route params.
- Fetches quote details from the API.
- Shows loading/error states.

## 5) Notes / Known Non-Functional Items

- The Angular build emits CSS budget warnings for the provided component CSS files. This does not affect runtime behavior.
- `npm install` reports vulnerabilities from upstream dependencies; addressing them is typically done via dependency upgrades and is outside the core technical-test functional scope.

## 6) How to Run

Prerequisites:

- Backend running on `http://localhost:8080`

Commands:

```bash
cd frontend
npm install
npm start
```

Then open:

- `http://localhost:4200`
