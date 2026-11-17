# Hourly Store — Backend

Spring Boot **3.2** + **MyBatis-Plus** + **Redis** / **Redisson** API for the Hourly Store flash-sale demo.

Layering: **Controller → Service → Mapper**. Covers auth, merchants, coupons, flash-sale orders, product catalog, and the concurrency pieces below.

## What’s in the box

| Area | Implementation |
|------|----------------|
| Sessions | Redis session tokens (`SessionService`); single active session per user |
| Cache | Cache-aside + random TTL jitter for merchant / coupon / flash-sale lists |
| Inventory | Redis **Lua** atomic stock decrement + one-purchase-per-user |
| Locks | **Redisson** for coupon claim and flash orders |
| Checkout spike | **Redis Streams** → async consumer → MySQL (auto-off on Redis &lt; 5) |
| Source of truth | MySQL for orders / claims / catalog |
| Auth | Phone or email login; verification codes in Redis (demo can return code in API) |
| Admin | Create / update flash-sale events (`/api/admin/flash-sales`) |
| Cache invalidation | Delayed double-delete after writes |

This repo stays focused on the flash-sale path (Lua + Streams + MySQL). It does **not** ship a full multi-node Nginx / RabbitMQ deployment.

## Prerequisites

- **JDK 17+**
- **Maven 3.8+**
- **MySQL 8** (`localhost:3306`)
- **Redis-compatible server** on **`localhost:6379`** (no password by default)

**Streams** (`XADD` / `XGROUP`) need **Redis 5.0+**. Older Windows Redis **3.0.x** will auto-disable Streams; set `hourlystore.streams.enabled: false` or install **Memurai** / another Redis 5+ build.

### Redis on Windows

See [docs/REDIS_WINDOWS.md](docs/REDIS_WINDOWS.md). Quick check:

```powershell
& "C:\Program Files\Redis\redis-cli.exe" ping
```

Expected: `PONG`.

## Database

```bash
mysql -u root -p < src/main/resources/schema.sql
```

Incremental scripts for older DBs: `src/main/resources/sql/`. Copy `application-example.yml` → `application.yml` and set MySQL / Redis.

## Run

```bash
cd hourly-store-backend
mvn spring-boot:run
```

Health: `GET http://localhost:8080/api/health`

## Frontend

Vue 3 app lives in `../hourly-store-frontend`. Dev: Vite proxies `/api` → `http://localhost:8080`. Production build lands in `src/main/resources/static` (same origin as the API).

```bash
# terminal 1
mvn spring-boot:run

# terminal 2
cd ../hourly-store-frontend
npm install
npm run dev
```

Or `npm run build` in the frontend, then open **http://localhost:8080/**.

**Demo admin:** phone `13800000000` / password `admin123`.

## API overview

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/auth/verification-code` | No — body `{ "phone" }` or `{ "email" }`; demo may return `demoCode` |
| POST | `/api/auth/register` | No — phone **or** email + `verificationCode` + password |
| POST | `/api/auth/login` | No — `{ "account", "password" }` → `token`, `role` |
| POST | `/api/admin/flash-sales` | Bearer **admin** |
| PUT | `/api/admin/flash-sales/{id}` | Bearer **admin** |
| GET | `/api/merchant-types` | No |
| GET | `/api/merchants?page=1&pageSize=10&merchantTypeId=` | No |
| GET | `/api/merchants/{id}` | No |
| GET | `/api/products?merchantId=1` | No |
| GET | `/api/products/{id}` | No |
| POST | `/api/products` | Bearer |
| GET | `/api/coupons?merchantId=1` | No |
| GET | `/api/flash-sales` | No |
| POST | `/api/coupons/{couponId}/claim` | Bearer |
| GET | `/api/me/coupon-claims` | Bearer |
| POST | `/api/products/{productId}/orders` | Bearer |
| POST | `/api/flash-sales/{eventId}/orders` | Bearer |
| GET | `/api/me/orders` | Bearer |

### Quick curl flow (after seed)

```bash
curl -s -X POST http://localhost:8080/api/auth/verification-code -H "Content-Type: application/json" -d "{\"phone\":\"13800000002\"}"

curl -s -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"phone\":\"13800000002\",\"password\":\"secret12\",\"nickname\":\"u2\",\"verificationCode\":\"123456\"}"

curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"account\":\"13800000002\",\"password\":\"secret12\"}"

curl -s http://localhost:8080/api/flash-sales

curl -s -X POST http://localhost:8080/api/flash-sales/1/orders -H "Authorization: Bearer YOUR_TOKEN" -H "Content-Type: application/json" -d "{}"
```

Seed data creates flash-sale event `id = 1` (coupon `id = 2`, 50 stock). One order per user per event (`uk_user_event`).

## Layout

```
src/main/java/com/hourlystore/
  controller/
  service/
  mapper/
  entity/
  dto/
  config/
```

SQL source of truth: `src/main/resources/schema.sql`.
