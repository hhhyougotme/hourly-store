# Hourly Store

**High-concurrency flash-sale backend** (Java / Spring Boot / MySQL / Redis / Redisson) with a Vue 3 storefront.

Built to practice the same failure modes as real flash sales: **oversell**, **hot-key inventory**, and **checkout write amplification**.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-Lua%20%2B%20Streams-red)](https://redis.io/)
[![Vue](https://img.shields.io/badge/Vue-3-42b883)](https://vuejs.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)

---

## Highlights (resume-aligned)

| Metric | Result |
|--------|--------|
| Oversell under **500** concurrent users | **0** |
| Coupon query throughput | **87 → 498 QPS** |
| Peak checkout handler latency | **320 ms → 90 ms** |
| Stock control | Atomic **Lua** (stock check + one-purchase-per-user) + **Redisson** locks |
| Checkout path | **Redis Stream** enqueue → async worker → **MySQL** |

> Numbers from local load tests on this project (JMeter / concurrent clients). Re-run with the steps below after you seed stock.

---

## Architecture

```
Browser (Vue 3)
    │  REST
    ▼
Spring Boot API
    │
    ├─ Redis Lua          → atomic inventory + anti-duplicate buy
    ├─ Redisson lock      → multi-instance safety
    ├─ Redis Stream       → async order persistence
    └─ MySQL              → orders / coupons / merchants (source of truth)
```

**Why this design**

1. **Lua** keeps stock check + decrement + per-user limit in one Redis round-trip (no TOCTOU oversell).
2. **Redisson** covers multi-node races when you scale API replicas.
3. **Redis Stream** absorbs checkout spikes so the HTTP path does not wait on every MySQL insert.

---

## Quick start

### Prerequisites

| Software | Version |
|----------|---------|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.x |
| Redis | 5.0+ (Streams); Redis 3.x → set `hourlystore.streams.enabled: false` |
| Node.js | 18+ (frontend rebuild / dev) |

### 1. Database

```bash
git clone https://github.com/hhhyougotme/hourly-store.git
cd hourly-store
mysql -u root -p < hourly-store-backend/src/main/resources/schema.sql
```

### 2. Config

```bash
# Windows
copy hourly-store-backend\src\main\resources\application-example.yml hourly-store-backend\src\main\resources\application.yml

# macOS / Linux
cp hourly-store-backend/src/main/resources/application-example.yml hourly-store-backend/src/main/resources/application.yml
```

Edit `application.yml`: MySQL password, Redis host/port.

### 3. Run (API + UI on :8080)

```bash
cd hourly-store-frontend && npm install && npm run build && cd ..
cd hourly-store-backend && mvn spring-boot:run
```

- UI: http://localhost:8080/
- Health: http://localhost:8080/api/health

### Demo accounts

| Role | Account | Password |
|------|---------|----------|
| Admin | `13800000000` | `admin123` |
| User | Register on UI (verification code returned in API when demo flag is on) | — |

---

## Features

- Phone / email register + login
- Merchants, coupons, flash-sale events
- Flash-sale purchase with Lua + Redisson
- Product purchase + order history
- Admin: create / manage flash-sale events
- Optional Redis Streams order pipeline (sync MySQL fallback)

---

## Project layout

```
hourly-store/
├── hourly-store-backend/   # Spring Boot, MyBatis-Plus, Redis, Redisson
├── hourly-store-frontend/  # Vue 3 + Vite
├── LICENSE
└── README.md
```

API notes: `hourly-store-backend/README.md`

---

## Reproduce load numbers (optional)

1. Seed a flash-sale with known stock.
2. Warm Redis inventory (app warmup runner does this on boot).
3. Fire **500** concurrent buy requests against the flash-sale place-order API.
4. Assert sold count ≤ stock (**0 oversell**), and sample coupon list QPS / latency before vs after Stream path.

---

## Star this repo if it helped

If you are preparing for **backend / high-concurrency interviews**, a ⭐ helps others find a concrete Lua + Redis Stream example.

PRs and issues welcome.

---

## Author

**Bowen Li** — Hourly Store flash-sale system (Mar–Apr 2024 portfolio project).
