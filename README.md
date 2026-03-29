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