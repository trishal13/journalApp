# Journal App - Flow Charts

## 1. Overall Architecture Flow

```mermaid
graph TB
    Client["🖥️ Client<br/>(Postman / Frontend)"]

    subgraph Security["Security Layer"]
        JwtFilter["JwtFilter<br/>Extract & validate JWT"]
        SecurityConfig["SecurityConfig<br/>Route authorization"]
    end

    subgraph Controllers["Controller Layer"]
        PublicCtrl["PublicController<br/>/public/**<br/>No auth required"]
        JournalCtrl["JournalEntryController<br/>/journal/**<br/>USER role"]
        UserCtrl["UserController<br/>/user/**<br/>USER role"]
        AdminCtrl["AdminController<br/>/admin/**<br/>ADMIN role"]
    end

    subgraph Services["Service Layer"]
        UserSvc["UserService"]
        JournalSvc["JournalEntryService"]
        GeminiSvc["GeminiService"]
        WeatherSvc["WeatherService"]
        RedisSvc["RedisService"]
        WeeklySvc["WeeklySentimentService"]
        EmailSvc["EmailService"]
    end

    subgraph External["External Services"]
        GeminiAPI["🤖 Gemini AI API"]
        WeatherAPI["🌤️ Weather API"]
        MailServer["📧 Mail Server"]
    end

    subgraph Data["Data Layer"]
        PostgreSQL[("🐘 PostgreSQL")]
        Redis[("⚡ Redis Cache")]
        Kafka["📨 Kafka"]
    end

    subgraph Background["Background Jobs"]
        Scheduler["UserScheduler<br/>Cron Jobs"]
        KafkaConsumer["SentimentKafkaConsumer"]
    end

    Client --> JwtFilter
    JwtFilter --> SecurityConfig
    SecurityConfig --> PublicCtrl
    SecurityConfig --> JournalCtrl
    SecurityConfig --> UserCtrl
    SecurityConfig --> AdminCtrl

    PublicCtrl --> UserSvc
    JournalCtrl --> JournalSvc
    JournalCtrl --> UserSvc
    UserCtrl --> UserSvc
    UserCtrl --> WeatherSvc
    AdminCtrl --> UserSvc
    AdminCtrl --> WeeklySvc

    UserSvc --> PostgreSQL
    JournalSvc --> PostgreSQL
    JournalSvc --> GeminiSvc
    GeminiSvc --> GeminiAPI
    WeatherSvc --> RedisSvc
    WeatherSvc --> WeatherAPI
    RedisSvc --> Redis
    WeeklySvc --> Kafka
    WeeklySvc --> EmailSvc
    EmailSvc --> MailServer
    KafkaConsumer --> Kafka
    KafkaConsumer --> EmailSvc
    Scheduler --> WeeklySvc

    style Security fill:#fff3cd,stroke:#ffc107
    style Controllers fill:#d1ecf1,stroke:#17a2b8
    style Services fill:#d4edda,stroke:#28a745
    style External fill:#f8d7da,stroke:#dc3545
    style Data fill:#e2d5f1,stroke:#6f42c1
    style Background fill:#fde2e2,stroke:#e74c3c
```

## 2. Authentication Flow (Signup → Login → Access)

```mermaid
sequenceDiagram
    actor User
    participant Public as PublicController
    participant UserSvc as UserService
    participant DB as PostgreSQL
    participant JWT as JwtUtil
    participant Filter as JwtFilter
    participant Journal as JournalEntryController

    Note over User,DB: 📝 SIGNUP
    User->>Public: POST /public/signup<br/>{ userName, password, email }
    Public->>UserSvc: saveNewUser(user)
    UserSvc->>DB: existsByUserName(userName)
    DB-->>UserSvc: false
    UserSvc->>UserSvc: BCrypt encode password
    UserSvc->>DB: save(user)
    DB-->>UserSvc: saved
    Public-->>User: 201 Created<br/>{ success: true, data: userResponse }

    Note over User,DB: 🔑 LOGIN
    User->>Public: POST /public/login<br/>{ userName, password }
    Public->>UserSvc: authenticate credentials
    UserSvc->>DB: findByUserName
    DB-->>UserSvc: user found
    Public->>JWT: generateToken(userName)
    JWT-->>Public: jwt_token
    Public-->>User: 200 OK<br/>{ success: true, data: { token, tokenType, roles } }

    Note over User,Journal: 📖 ACCESS PROTECTED ENDPOINT
    User->>Filter: GET /journal<br/>Authorization: Bearer jwt_token
    Filter->>JWT: extractUserName(token)
    JWT-->>Filter: userName
    Filter->>JWT: validateToken(token)
    JWT-->>Filter: true
    Filter->>Filter: Set SecurityContext
    Filter->>Journal: Forward request
    Journal-->>User: 200 OK<br/>{ success: true, data: [...entries] }
```

## 3. Journal Entry Creation Flow (with Sentiment Analysis)

```mermaid
sequenceDiagram
    actor User
    participant Ctrl as JournalEntryController
    participant Mapper as JournalEntryMapper
    participant Svc as JournalEntryService
    participant UserSvc as UserService
    participant Gemini as GeminiService
    participant AI as Gemini AI API
    participant DB as PostgreSQL

    User->>Ctrl: POST /journal<br/>{ title, content }
    Ctrl->>Mapper: toEntity(createDto)
    Mapper-->>Ctrl: JournalEntry entity
    Ctrl->>Svc: saveEntry(entry, userName)

    Svc->>UserSvc: findByUserName(userName)
    UserSvc->>DB: findByUserName
    DB-->>UserSvc: User
    UserSvc-->>Svc: User

    Note over Svc,AI: 🤖 Sentiment Analysis
    Svc->>Gemini: analyseSentiment(title + content)
    Gemini->>AI: POST generateContent<br/>"Analyze sentiment..."
    AI-->>Gemini: "HAPPY"
    Gemini-->>Svc: Sentiment.HAPPY

    Svc->>Svc: entry.setSentiment(HAPPY)
    Svc->>Svc: entry.setUser(user)
    Svc->>DB: save(entry)
    DB-->>Svc: saved

    Svc-->>Ctrl: done
    Ctrl->>Mapper: toResponse(entry)
    Mapper-->>Ctrl: JournalEntryResponseDto
    Ctrl-->>User: 201 Created<br/>{ success: true, data: { id, title, sentiment: HAPPY } }
```

## 4. Weather Greeting Flow (with Redis Cache)

```mermaid
sequenceDiagram
    actor User
    participant Ctrl as UserController
    participant WeatherSvc as WeatherService
    participant Redis as RedisService
    participant Cache as Redis
    participant API as Weather API

    User->>Ctrl: GET /user?city=Mumbai
    Ctrl->>WeatherSvc: getWeather("Mumbai")

    Note over WeatherSvc,Cache: Check cache first
    WeatherSvc->>Redis: get("weather_of_mumbai")
    Redis->>Cache: GET weather_of_mumbai
    Cache-->>Redis: null (cache miss)
    Redis-->>WeatherSvc: null

    Note over WeatherSvc,API: Cache miss → call external API
    WeatherSvc->>API: GET weather?city=Mumbai
    API-->>WeatherSvc: { current: { feelslike: 32 } }

    Note over WeatherSvc,Cache: Cache the result (5 min TTL)
    WeatherSvc->>Redis: set("weather_of_mumbai", response, 300)
    Redis->>Cache: SET weather_of_mumbai (TTL 300s)

    WeatherSvc-->>Ctrl: WeatherResponse
    Ctrl-->>User: 200 OK<br/>{ data: "Hi johndoe, Weather feels like: 32°C" }
```

## 5. Weekly Sentiment Report Flow

```mermaid
sequenceDiagram
    participant Cron as UserScheduler<br/>(Sunday 9AM)
    participant Svc as WeeklySentimentService
    participant DB as PostgreSQL
    participant Kafka as Kafka Producer
    participant Topic as Kafka Topic
    participant Consumer as SentimentKafkaConsumer
    participant Email as EmailService
    participant Mail as Mail Server

    Cron->>Svc: runWeeklySentimentReport()
    Svc->>DB: getUsersForSentimentAnalysis()
    DB-->>Svc: [user1, user2, ...]

    loop For each user
        Svc->>Svc: getEntriesFromLastSevenDays(user)
        Svc->>Svc: findDominantSentiment(entries)
        Svc->>Svc: buildSummaryMessage(user, entries, sentiment)

        alt Kafka available
            Svc->>Kafka: sendWeeklySentiment(sentimentData)
            Kafka->>Topic: publish message
            Topic->>Consumer: consume message
            Consumer->>Email: sendEmail(to, subject, body)
            Email->>Mail: send
        else Kafka unavailable
            Svc->>Email: sendEmail(to, subject, body) [fallback]
            Email->>Mail: send
        end
    end
```

## 6. Error Handling Flow

```mermaid
graph TD
    Request["Incoming Request"] --> Controller
    Controller --> |"throws exception"| GEH["GlobalExceptionHandler"]

    GEH --> |"JournalAppException"| JA["Map ErrorCode → HTTP status<br/>Return ApiResponse with error details"]
    GEH --> |"MethodArgumentNotValidException"| VA["400 Bad Request<br/>Field-level validation errors"]
    GEH --> |"HttpMessageNotReadableException"| MR["400 Bad Request<br/>ERR_6002: Malformed request body"]
    GEH --> |"AccessDeniedException"| AD["403 Forbidden<br/>ERR_5003: Access denied"]
    GEH --> |"AuthenticationException"| AU["401 Unauthorized<br/>ERR_1006: Invalid credentials"]
    GEH --> |"Any other Exception"| GE["500 Internal Server Error<br/>ERR_9001: Unexpected error"]

    JA --> Response["Unified ApiResponse<br/>{ success, message, data, errors, metadata }"]
    VA --> Response
    MR --> Response
    AD --> Response
    AU --> Response
    GE --> Response

    style GEH fill:#fff3cd,stroke:#ffc107
    style Response fill:#d4edda,stroke:#28a745
```

## 7. API Endpoint Map

```mermaid
graph LR
    subgraph Public["🔓 Public (No Auth)"]
        HC["GET /public/health-check"]
        SU["POST /public/signup"]
        LI["POST /public/login"]
    end

    subgraph User["🔐 User (JWT Required)"]
        JList["GET /journal"]
        JCreate["POST /journal"]
        JGet["GET /journal/id/{id}"]
        JUpdate["PUT /journal/id/{id}"]
        JDelete["DELETE /journal/id/{id}"]
        UGreet["GET /user?city=X"]
        UUpdate["PUT /user"]
        UDelete["DELETE /user"]
    end

    subgraph Admin["🛡️ Admin (ADMIN Role)"]
        AUsers["GET /admin/all-users"]
        ACreate["POST /admin/create-admin-user"]
        ACache["GET /admin/clear-app-cache"]
        ASentiment["POST /admin/trigger-weekly-sentiment"]
    end

    style Public fill:#d4edda,stroke:#28a745
    style User fill:#d1ecf1,stroke:#17a2b8
    style Admin fill:#f8d7da,stroke:#dc3545
```
