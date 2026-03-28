# Journal App - High Level Design (HLD)

## 1. System Overview

```mermaid
graph TB
    subgraph Clients["Clients"]
        Postman["Postman / REST Client"]
        Browser["Browser (Swagger UI)"]
    end

    subgraph AppServer["Application Server (Spring Boot)"]
        API["REST API Layer<br/>Port 8080"]
        Auth["JWT Authentication"]
        BL["Business Logic"]
        Scheduler["Cron Scheduler"]
    end

    subgraph DataStores["Data Stores"]
        PG[("PostgreSQL<br/>Primary Database")]
        Redis[("Redis<br/>Cache Layer")]
    end

    subgraph MessageBroker["Message Broker"]
        Kafka["Apache Kafka<br/>Event Streaming"]
    end

    subgraph ExternalAPIs["External APIs"]
        Gemini["Google Gemini AI<br/>Sentiment Analysis"]
        Weather["Weatherstack API<br/>Weather Data"]
        SMTP["SMTP Server<br/>Email Delivery"]
    end

    Clients --> API
    API --> Auth
    Auth --> BL
    BL --> PG
    BL --> Redis
    BL --> Kafka
    BL --> Gemini
    BL --> Weather
    BL --> SMTP
    Scheduler --> BL
    Kafka --> BL

    style AppServer fill:#d1ecf1,stroke:#17a2b8
    style DataStores fill:#e2d5f1,stroke:#6f42c1
    style MessageBroker fill:#fff3cd,stroke:#ffc107
    style ExternalAPIs fill:#f8d7da,stroke:#dc3545
```

## 2. Component Responsibilities

| Component | Role |
|-----------|------|
| Spring Boot App | Hosts REST API, business logic, scheduling |
| PostgreSQL | Stores users, journal entries, app config |
| Redis | Caches weather API responses (5 min TTL) |
| Apache Kafka | Async event streaming for weekly sentiment reports |
| Gemini AI | Analyses journal entry text → returns sentiment (HAPPY/SAD/ANGRY/ANXIOUS) |
| OpenWeatherMap API | Provides current weather data by lat/lon coordinates |
| SMTP | Delivers weekly sentiment summary emails |

## 3. High-Level Request Flow

```mermaid
graph LR
    A["Client Request"] --> B["JwtFilter<br/>Token validation"]
    B --> C["SecurityConfig<br/>Route authorization"]
    C --> D["Controller<br/>Request handling"]
    D --> E["Service<br/>Business logic"]
    E --> F["Repository<br/>Data access"]
    F --> G[("PostgreSQL")]
    E --> H[("Redis")]
    E --> I["External APIs"]

    D --> J["GlobalExceptionHandler"]
    J --> K["Unified ApiResponse"]
    D --> K
```

## 4. Data Flow Patterns

```mermaid
graph TD
    subgraph Sync["Synchronous Flows"]
        S1["Signup/Login → DB write → JWT response"]
        S2["Create Entry → Gemini AI → DB write → response"]
        S3["Weather Greeting → Redis check → API call → cache → response"]
    end

    subgraph Async["Asynchronous Flows"]
        A1["Cron (Sunday 9AM) → Aggregate sentiments → Kafka publish"]
        A2["Kafka consume → Send email via SMTP"]
        A3["Cron (every 10 min) → Refresh app config cache"]
    end

    style Sync fill:#d4edda,stroke:#28a745
    style Async fill:#fff3cd,stroke:#ffc107
```

## 5. Security Architecture

```mermaid
graph TD
    Request["Incoming Request"] --> Filter["JwtFilter"]

    Filter --> |"No token"| Public["Public endpoints only<br/>/public/**, /swagger-ui/**"]
    Filter --> |"Valid token"| SecCtx["Set SecurityContext"]
    Filter --> |"Expired token"| Reject1["401 Unauthorized"]
    Filter --> |"Invalid token"| Reject2["401 Unauthorized"]

    SecCtx --> RoleCheck{"Role Check"}
    RoleCheck --> |"USER"| UserEndpoints["/journal/**, /user/**"]
    RoleCheck --> |"ADMIN"| AdminEndpoints["/admin/**"]
    RoleCheck --> |"Insufficient"| Reject3["403 Forbidden"]

    style Public fill:#d4edda,stroke:#28a745
    style UserEndpoints fill:#d1ecf1,stroke:#17a2b8
    style AdminEndpoints fill:#f8d7da,stroke:#dc3545
```

## 6. Deployment Architecture

```mermaid
graph TB
    subgraph Docker["Docker Compose"]
        App["journal-app<br/>Spring Boot<br/>:8080"]
        PG["postgres<br/>PostgreSQL<br/>:5432"]
        RedisC["redis<br/>Redis<br/>:6379"]
        KafkaC["kafka<br/>Apache Kafka<br/>:9092"]
    end

    App --> PG
    App --> RedisC
    App --> KafkaC
    App --> Internet["Internet<br/>(Gemini AI, Weather API, SMTP)"]

    style Docker fill:#d1ecf1,stroke:#17a2b8
```

## 7. API Surface Summary

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /public/health-check | None | Health check |
| POST | /public/signup | None | Register new user |
| POST | /public/login | None | Login, get JWT |
| GET | /journal | JWT | List user's entries |
| POST | /journal | JWT | Create entry (auto-sentiment) |
| GET | /journal/id/{id} | JWT | Get entry by ID |
| PUT | /journal/id/{id} | JWT | Update entry |
| DELETE | /journal/id/{id} | JWT | Delete entry |
| GET | /user?lat=X&lon=Y | JWT | Greeting + weather |
| PUT | /user | JWT | Update profile |
| DELETE | /user | JWT | Delete account |
| GET | /admin/all-users | ADMIN | List all users |
| POST | /admin/create-admin-user | ADMIN | Create admin |
| GET | /admin/clear-app-cache | ADMIN | Refresh config cache |
| POST | /admin/trigger-weekly-sentiment | ADMIN | Force sentiment report |
| GET | /admin/configs | ADMIN | List all configs |
| PUT | /admin/configs/{key} | ADMIN | Update config value |
| PUT | /admin/users/{username} | ADMIN | Update any user |
| GET | /admin/users/{username}/journals | ADMIN | View user's journals |