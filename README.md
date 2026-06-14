# URL Shortener System

Scalable two-service URL shortener built with Spring Boot, Zookeeper, Redis, and PostgreSQL.

## Architecture

```
Client
  │
  ├─ POST /api/v1/urls  ──► url-shortener-service (:8080)
  │                              │
  │                              ├─ Zookeeper → assign workerId
  │                              ├─ Snowflake ID → unique 64-bit int
  │                              ├─ Base62 encode → 6-7 char key
  │                              └─ PostgreSQL + Redis (write)
  │
  └─ GET /{shortKey}    ──► url-redirect-service (:8081)
                               │
                               ├─ Redis cache (L1, ~1ms)
                               ├─ PostgreSQL (L2, cache miss)
                               └─ HTTP 302 → longUrl
```

## Snowflake ID Layout

```
 63        22       12        0
  │         │        │        │
  0│timestamp│workerId│sequence│
  1   41 bits  10 bits  12 bits
```

- **Sign bit (1)**: always 0 → positive long
- **Timestamp (41 bits)**: ms since 2024-01-01, works until ~2093
- **Worker ID (10 bits)**: 0–1023 unique workers, assigned by Zookeeper
- **Sequence (12 bits)**: 0–4095 IDs per millisecond per worker

Max throughput: **4,096,000 IDs/second** globally (1024 workers × 4096/ms)

## Quick Start

```bash
# Start the full stack
docker compose up -d

# Shorten a URL
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://example.com/very/long/path"}'

# Response:
# {
#   "shortKey": "dU3kR9",
#   "shortUrl": "http://localhost:8081/dU3kR9",
#   "longUrl": "https://example.com/very/long/path",
#   "snowflakeId": 7234567890123456789
# }

# Follow the redirect
curl -L http://localhost:8081/dU3kR9

# Scale redirect service independently (handles 10× more traffic)
docker compose up --scale redirect=5 -d
```

## Project Structure

```
url-shortener-system/
├── docker-compose.yml
├── shared-schema/
│   └── V1__initial_schema.sql        # PostgreSQL schema
│
├── url-shortener-service/            # POST: encode service (:8080)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/urlshortener/shortener/
│       ├── UrlShortenerApplication.java
│       ├── config/
│       │   ├── ZookeeperConfig.java  # Curator client bean
│       │   └── AppConfig.java        # Redis cache config
│       ├── controller/
│       │   └── UrlShortenerController.java
│       ├── service/
│       │   └── UrlShortenerService.java
│       ├── util/
│       │   ├── SnowflakeIdGenerator.java  # Core ID generation
│       │   ├── Base62Encoder.java         # ID → short key
│       │   └── ZookeeperWorkerRegistry.java  # Worker ID claim
│       ├── model/
│       │   ├── UrlMapping.java
│       │   └── User.java
│       ├── repository/
│       │   ├── UrlMappingRepository.java
│       │   └── UserRepository.java
│       ├── dto/
│       │   ├── ShortenRequest.java
│       │   ├── ShortenResponse.java
│       │   └── ErrorResponse.java
│       └── exception/
│           ├── GlobalExceptionHandler.java
│           ├── UrlNotFoundException.java
│           └── DuplicateUrlException.java
│
└── url-redirect-service/             # GET: decode + redirect (:8081)
    ├── pom.xml
    ├── Dockerfile
    └── src/main/java/com/urlshortener/redirect/
        ├── UrlRedirectApplication.java
        ├── config/
        │   ├── AsyncConfig.java      # Thread pool for click tracking
        │   └── RedisConfig.java
        ├── controller/
        │   └── RedirectController.java
        ├── service/
        │   └── RedirectService.java
        ├── model/
        │   └── UrlMapping.java       # Read-only mirror
        ├── repository/
        │   └── UrlMappingRepository.java
        └── exception/
            ├── GlobalExceptionHandler.java
            ├── ShortUrlNotFoundException.java
            └── ShortUrlExpiredException.java
```

## Key Design Decisions

| Decision | Choice | Why |
|---|---|---|
| ID generation | Snowflake | No DB round-trip, time-sortable, globally unique |
| Short key encoding | Base62 | URL-safe, 6-7 chars for any Snowflake ID |
| Worker coordination | Zookeeper (Curator) | Industry standard; ephemeral nodes auto-release |
| Cache | Redis | Shared across all redirect instances; survives restarts |
| Two services | Separate deployments | Redirect traffic is 100× higher; scale independently |
| Click tracking | Async thread pool | Never blocks the 302 redirect |
