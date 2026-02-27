# Order Management Demo

A Spring Boot REST API demonstrating Role-Based Access Control, JWT Authentication, Redis Caching, Optimistic Locking, and dynamic order filtering with pagination.

## Tech Stack

- Java 17, Spring Boot 3.4.3
- MySQL 8.0 — persistent order and user data
- Redis 7 — caching for order lookups
- JWT — stateless authentication (1 hour expiry)
- Docker & Docker Compose — fully containerised setup

## Features

- JWT-based auth with two roles: `ADMIN` and `USER`
- Create, retrieve, filter, and paginate orders
- Role-scoped data access — users can only see their own orders
- Redis caching on `GET /api/orders/{id}` with 60s TTL
- Optimistic locking on order status updates (handles concurrent writes with 409)
- Bean Validation on all request DTOs
- Swagger UI for interactive API exploration

## Prerequisites

1. [Docker Desktop](https://www.docker.com/products/docker-desktop/) — ensure it is installed and running
2. [Git](https://git-scm.com/downloads) — to clone the repository

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/vaibhav-gotatva/Order-Management-Demo.git
cd Order-Management-Demo/demo
```

### 2. Create the `.env` file

Create a `.env` file inside the `demo/` directory with the following content:

```env
DB_HOST=mysql
DB_PORT=3306
DB_NAME=order_management_db
DB_USERNAME=root
DB_PASSWORD=your_password

REDIS_HOST=redis
REDIS_PORT=6379

JWT_SECRET=your_jwt_secret_key_at_least_32_chars
```

> **Note:** The `DB_HOST` and `REDIS_HOST` values must remain `mysql` and `redis` — these are the Docker Compose service names used for internal container networking.

### 3. Start the application

```bash
docker-compose up --build -d
```

This starts three containers:
- `app` — the Spring Boot application on port `8080`
- `mysql` — MySQL database on port `3307` (host) → `3306` (container)
- `redis` — Redis on port `6379`

The app waits for MySQL to pass its health check before starting.

### 4. Access the API

- **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

## API Overview

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | Public | Register a new user |
| POST | `/api/auth/login` | Public | Login and receive a JWT |
| POST | `/api/orders` | ADMIN, USER | Create an order |
| GET | `/api/orders/{id}` | ADMIN, USER | Get order by ID (Redis cached) |
| GET | `/api/orders` | ADMIN, USER | List orders with filters and pagination |
| PATCH | `/api/orders/{id}/status` | ADMIN | Update order status |
| GET | `/api/orders/{userId}/order-count` | ADMIN, USER | Get order count for a user |
| GET | `/api/orders/{userId}/recent-orders` | ADMIN, USER | Get recent orders for a user |

## Stopping the application

```bash
docker-compose down
```

To also remove volumes (database data):

```bash
docker-compose down -v
```
