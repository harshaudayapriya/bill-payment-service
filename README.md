# Bill Payments Service

A production-grade bill payment purchase transaction service built with **Java 21**, **Spring Boot 4.0.0**, **Maven**,
and **H2 (file-based)** database.

## Features

### 1. Create Purchase Transaction

- **POST** `/api/v1/transactions`
- Validates: description (max 50 chars), valid date, positive amount (rounded to nearest cent), biller type
- Returns the created transaction with a unique UUID identifier

### 2. Retrieve Transaction in a Specified Currency

- **GET** `/api/v1/transactions/{id}/convert?currency=Canada-Dollar`
- Fetches exchange rate from [US Treasury Fiscal Data API]
- Matches the exchange rate to the nearest available date to the transaction date
- Returns original amount, exchange rate, converted amount, and rate date

### 3. Additional Endpoints

- **GET** `/api/v1/transactions/{id}` — Retrieve a transaction by ID
- **GET** `/api/v1/transactions` — List all transactions (paginated)

## Prerequisites

| Requirement | Version                                        |
|-------------|------------------------------------------------|
| Java JDK    | 21+                                            |
| Maven       | 3.9+ (or use the included `mvnw` wrapper)      |
| Docker      | 24+ (optional — only for container deployment) |

> **Note:** You do NOT need to install Maven separately. The project includes the Maven Wrapper (`mvnw`).

---

## Quick Start

```bash
# Clone and enter the project
cd bill-payment-service

# Build the application
./mvnw clean package

# Run the application




## API Usage Examples

### Create a Purchase Transaction

```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -H "X-API-Key: test-api-key" \
  -d '{
    "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
    "description": "Electric bill March 2025",
    "transactionDate": "2025-03-15",
    "purchaseAmount": 150.75,
    "billerType": "ELECTRICITY"
  }'
```

**Response (201 Created):**

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "description": "Electric bill March 2025",
  "transactionDate": "2025-03-15",
  "purchaseAmount": 150.75,
  "billerType": "ELECTRICITY",
  "createdAt": "2025-03-15T10:30:00"
}
```

### Get Transaction Converted to a Target Currency

```bash
curl -H "X-API-Key: test-api-key" \
  "http://localhost:8080/api/v1/transactions/{id}/convert?currency=Canada-Dollar"
```

**Response (200 OK):**

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "description": "Electric bill March 2025",
  "transactionDate": "2025-03-15",
  "billerType": "ELECTRICITY",
  "originalAmountUsd": 150.75,
  "exchangeRate": 1.35,
  "convertedAmount": 203.51,
  "targetCurrency": "Canada-Dollar",
  "exchangeRateDate": "2025-03-01"
}
```

### Supported Currency Values

These are the `currency` parameter values accepted by the Treasury Reporting Rates of Exchange API:

| Currency Parameter     | Description     |
|------------------------|-----------------|
| `Canada-Dollar`        | Canadian Dollar |
| `Euro Zone-Euro`       | Euro            |
| `United Kingdom-Pound` | British Pound   |

### Supported Biller Types

`ELECTRICITY`, `WATER`, `GAS`, `INTERNET`, `TELEPHONE`, `CABLE_TV`, `INSURANCE`, `CREDIT_CARD`, `MORTGAGE`, `EDUCATION`,
`GOVERNMENT`, `OTHER`

## Swagger UI / OpenAPI

- **Swagger UI (interactive):** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON spec:** http://localhost:8080/api-docs

Swagger UI is publicly accessible (no auth required). To test API endpoints from Swagger UI, click the **"Authorize"**
button and enter your API key in the `X-API-Key` field.

---

## H2 Database Console

Access the web-based database console at: http://localhost:8080/h2-console

| Setting  | Value                                |
|----------|--------------------------------------|
| JDBC URL | `jdbc:h2:file:./data/billpaymentdb;` |
| Username | `admin`                              |
| Password | 1234                                 |