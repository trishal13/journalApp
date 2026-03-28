# Journal App - Low Level Design (LLD)

## 1. Package Structure

```mermaid
graph TD
    Root["com.trishal.journalApp"]

    Root --> controller["controller"]
    Root --> service["service"]
    Root --> repository["repository"]
    Root --> entity["entity"]
    Root --> dto["dto"]
    Root --> mapper["mapper"]
    Root --> config["config"]
    Root --> filter["filter"]
    Root --> exception["exception"]
    Root --> enums["enums"]
    Root --> utils["utils"]
    Root --> cache["cache"]
    Root --> kafka["kafka"]
    Root --> api["api"]
    Root --> model["model"]
    Root --> scheduler["scheduler"]
    Root --> constants["constants"]

    controller --> PublicCtrl["PublicController"]
    controller --> JournalCtrl["JournalEntryController"]
    controller --> UserCtrl["UserController"]
    controller --> AdminCtrl["AdminController"]

    service --> UserSvc["UserService"]
    service --> JournalSvc["JournalEntryService"]
    service --> GeminiSvc["GeminiService"]
    service --> WeatherSvc["WeatherService"]
    service --> RedisSvc["RedisService"]
    service --> EmailSvc["EmailService"]
    service --> WeeklySvc["WeeklySentimentService"]
    service --> impl["impl/UserDetailServiceImpl"]
```

## 2. Entity Relationship Diagram

```mermaid
erDiagram
    USERS {
        UUID userId PK
        String userName UK "NOT NULL, UNIQUE"
        String password "NOT NULL, BCrypt encoded"
        String email
        boolean sentimentAnalysis
    }

    JOURNAL_ENTRIES {
        UUID id PK
        String title "NOT NULL"
        String content
        Date date "AUTO: @CreationTimestamp"
        Sentiment sentiment "HAPPY|SAD|ANGRY|ANXIOUS"
        UUID user_id FK "NOT NULL"
    }

    USER_ROLES {
        UUID user_id FK
        String role "USER or ADMIN"
    }

    CONFIG_JOURNAL_APP {
        UUID id PK
        String key
        String value
    }

    USERS ||--o{ JOURNAL_ENTRIES : "has many"
    USERS ||--o{ USER_ROLES : "has many"
```

## 3. Class Diagram - Core Entities

```mermaid
classDiagram
    class User {
        -UUID userId
        -String userName
        -String password
        -String email
        -boolean sentimentAnalysis
        -List~JournalEntry~ journalEntries
        -List~String~ roles
    }

    class JournalEntry {
        -UUID id
        -String title
        -String content
        -Date date
        -Sentiment sentiment
        -User user
    }

    class Sentiment {
        <<enumeration>>
        HAPPY
        SAD
        ANGRY
        ANXIOUS
    }

    User "1" --> "*" JournalEntry : owns
    JournalEntry --> Sentiment : has
```

## 4. Class Diagram - Service Layer

```mermaid
classDiagram
    class UserService {
        -UserRepo userRepo
        -PasswordEncoder passwordEncoder
        +saveNewUser(User) void
        +saveAdmin(User) void
        +updateUser(User, UserUpdateRequestDto) User
        +saveEntry(User) void
        +findByUserName(String) User
        +getAll() List~User~
        +deleteByUserName(String) void
    }

    class JournalEntryService {
        -JournalEntryRepo journalEntryRepo
        -UserService userService
        -GeminiService geminiService
        +saveEntry(JournalEntry, String) void
        +saveEntry(JournalEntry, User) void
        +getJournalEntryById(UUID) Optional~JournalEntry~
        +deleteJournalEntryById(UUID, String) boolean
        -analyseSentimentSafely(JournalEntry) Sentiment
        -buildTextForAnalysis(JournalEntry) String
    }

    class GeminiService {
        -RestTemplate restTemplate
        -String apiKey
        +analyseSentiment(String) Sentiment
        -parseSentiment(String) Sentiment
        -sanitise(String) String
    }

    class WeatherService {
        -RestTemplate restTemplate
        -AppCache appCache
        -RedisService redisService
        -String apiKey
        +getWeather(double lat, double lon) WeatherResponse
    }

    class WeeklySentimentService {
        -UserRepoImpl userRepoImpl
        -EmailService emailService
        -SentimentKafkaProducer producer
        +runWeeklySentimentReport() int
        -processUser(User) void
        -getEntriesFromLastSevenDays(User) List
        -findDominantSentiment(List) Optional~Sentiment~
        -buildSummaryMessage(User, List, Sentiment) String
    }

    JournalEntryService --> UserService : uses
    JournalEntryService --> GeminiService : uses
    WeatherService --> RedisService : uses
    WeeklySentimentService --> EmailService : uses
    WeeklySentimentService --> SentimentKafkaProducer : uses
```

## 5. DTO Layer

```mermaid
classDiagram
    class ApiResponse~T~ {
        -boolean success
        -String message
        -T data
        -List~ApiError~ errors
        -Map~String,Object~ metadata
        +success(T, String)$ ApiResponse
        +success(T)$ ApiResponse
        +success(String)$ ApiResponse
        +error(String, List)$ ApiResponse
        +error(String)$ ApiResponse
    }

    class ApiError {
        -String code
        -String message
        -String field
    }

    class UserRegistrationRequestDto {
        -String userName "@NotBlank, @Size(3-50)"
        -String password "@NotBlank, @Size(8-100)"
        -String email "@Email"
        -boolean sentimentAnalysis
    }

    class UserLoginRequestDto {
        -String userName "@NotBlank"
        -String password "@NotBlank"
    }

    class UserLoginResponseDto {
        -String token
        -String tokenType
        -String userName
        -List~String~ roles
        -long expiresIn
    }

    class UserResponseDto {
        -UUID userId
        -String userName
        -String email
        -boolean sentimentAnalysis
        -List~String~ roles
        -int journalEntryCount
    }

    class JournalEntryCreateRequestDto {
        -String title "@NotBlank, @Size(1-200)"
        -String content "@Size(max=10000)"
        -Sentiment sentiment
    }

    class JournalEntryUpdateRequestDto {
        -String title
        -String content
        -Sentiment sentiment
    }

    class JournalEntryResponseDto {
        -UUID id
        -String title
        -String content
        -Date date
        -Sentiment sentiment
        -String authorUserName
    }

    ApiResponse --> ApiError : contains
```

## 6. Exception Hierarchy

```mermaid
classDiagram
    class RuntimeException {
        <<Java>>
    }

    class JournalAppException {
        -ErrorCode errorCode
        +JournalAppException(ErrorCode)
        +JournalAppException(ErrorCode, String)
        +JournalAppException(ErrorCode, Throwable)
    }

    class UserNotFoundException {
        +UserNotFoundException(String username)
    }

    class JournalEntryNotFoundException {
        +JournalEntryNotFoundException(UUID id)
    }

    class JournalEntryAccessDeniedException {
        +JournalEntryAccessDeniedException(UUID id)
    }

    class ErrorCode {
        <<enumeration>>
        -String code
        -String message
        -HttpStatus httpStatus
        USER_NOT_FOUND: ERR_1001 - 404
        USER_ALREADY_EXISTS: ERR_1002 - 409
        USER_CREATION_FAILED: ERR_1003 - 500
        USER_UPDATE_FAILED: ERR_1004 - 500
        USER_DELETION_FAILED: ERR_1005 - 500
        USER_INVALID_CREDENTIALS: ERR_1006 - 401
        JOURNAL_ENTRY_NOT_FOUND: ERR_2001 - 404
        JOURNAL_ENTRY_ACCESS_DENIED: ERR_2002 - 403
        JOURNAL_ENTRY_CREATION_FAILED: ERR_2003 - 500
        JOURNAL_ENTRY_UPDATE_FAILED: ERR_2004 - 500
        JOURNAL_ENTRY_DELETION_FAILED: ERR_2005 - 500
        SENTIMENT_ANALYSIS_FAILED: ERR_3001 - 500
        SENTIMENT_INVALID: ERR_3002 - 400
        WEATHER_SERVICE_UNAVAILABLE: ERR_4001 - 503
        GEMINI_SERVICE_UNAVAILABLE: ERR_4002 - 503
        EMAIL_SEND_FAILED: ERR_4003 - 500
        KAFKA_PUBLISH_FAILED: ERR_4004 - 500
        JWT_INVALID: ERR_5001 - 401
        JWT_EXPIRED: ERR_5002 - 401
        ACCESS_DENIED: ERR_5003 - 403
        VALIDATION_FAILED: ERR_6001 - 400
        INVALID_REQUEST_BODY: ERR_6002 - 400
        INTERNAL_SERVER_ERROR: ERR_9001 - 500
    }

    RuntimeException <|-- JournalAppException
    JournalAppException <|-- UserNotFoundException
    JournalAppException <|-- JournalEntryNotFoundException
    JournalAppException <|-- JournalEntryAccessDeniedException
    JournalAppException --> ErrorCode : uses
```

## 7. JWT Authentication Sequence (Detailed)

```mermaid
sequenceDiagram
    actor Client
    participant Filter as JwtFilter
    participant JwtUtil as JwtUtil
    participant UDS as UserDetailServiceImpl
    participant SecCtx as SecurityContext
    participant Controller as Controller

    Client->>Filter: HTTP Request + Authorization: Bearer <token>

    Filter->>Filter: Extract token from header

    alt Token present
        Filter->>JwtUtil: extractUserName(token)

        alt Token expired
            JwtUtil-->>Filter: throw ExpiredJwtException
            Filter->>Filter: log "JWT expired"
            Filter->>Controller: Continue (unauthenticated)
        else Token malformed
            JwtUtil-->>Filter: throw Exception
            Filter->>Filter: log "JWT invalid"
            Filter->>Controller: Continue (unauthenticated)
        else Token valid
            JwtUtil-->>Filter: userName
            Filter->>JwtUtil: validateToken(token, userName)
            JwtUtil-->>Filter: true

            Filter->>UDS: loadUserByUsername(userName)
            UDS-->>Filter: UserDetails (with roles)

            Filter->>SecCtx: setAuthentication(user, roles)
            Filter->>Controller: Continue (authenticated)
        end
    else No token
        Filter->>Controller: Continue (unauthenticated)
    end

    Controller-->>Client: Response
```

## 8. Journal Entry Create - Detailed Sequence

```mermaid
sequenceDiagram
    actor Client
    participant Ctrl as JournalEntryController
    participant Mapper as JournalEntryMapper
    participant Svc as JournalEntryService
    participant UserSvc as UserService
    participant DB as PostgreSQL
    participant Gemini as GeminiService
    participant AI as Gemini AI

    Client->>Ctrl: POST /journal { title, content }
    Ctrl->>Ctrl: getAuthenticatedUserName() from SecurityContext
    Ctrl->>Mapper: toEntity(createDto)
    Mapper-->>Ctrl: JournalEntry (no user, no sentiment yet)

    Ctrl->>Svc: saveEntry(entry, userName)
    Svc->>UserSvc: findByUserName(userName)
    UserSvc->>DB: SELECT * FROM users WHERE userName = ?
    DB-->>UserSvc: User
    UserSvc-->>Svc: User

    Note over Svc,AI: Sentiment Analysis (safe)
    Svc->>Svc: buildTextForAnalysis(entry)<br/>"title. content"
    Svc->>Gemini: analyseSentiment(text)
    Gemini->>Gemini: sanitise(text) - strip quotes/newlines
    Gemini->>Gemini: Build GeminiRequest with prompt
    Gemini->>AI: POST /v1beta/models/gemini-2.0-flash:generateContent
    AI-->>Gemini: { candidates: [{ content: { parts: [{ text: "HAPPY" }] } }] }
    Gemini->>Gemini: parseSentiment("HAPPY") → Sentiment.HAPPY
    Gemini-->>Svc: Sentiment.HAPPY

    Svc->>Svc: entry.setSentiment(HAPPY)
    Svc->>Svc: entry.setUser(user)
    Svc->>DB: INSERT INTO journal_entries (...)
    DB-->>Svc: saved

    Svc-->>Ctrl: done
    Ctrl->>Mapper: toResponse(entry)
    Mapper-->>Ctrl: JournalEntryResponseDto
    Ctrl-->>Client: 201 { success: true, data: { id, title, sentiment: HAPPY } }
```

## 9. Weekly Sentiment Report - Detailed Sequence

```mermaid
sequenceDiagram
    participant Cron as UserScheduler<br/>(Sunday 9AM)
    participant Svc as WeeklySentimentService
    participant Repo as UserRepoImpl
    participant DB as PostgreSQL
    participant Producer as SentimentKafkaProducer
    participant Kafka as Kafka Topic
    participant Consumer as SentimentKafkaConsumer
    participant Email as EmailService
    participant SMTP as Mail Server

    Cron->>Svc: runWeeklySentimentReport()

    Svc->>Repo: getUsersForSentimentAnalysis()
    Repo->>DB: SELECT * FROM users<br/>WHERE email IS NOT NULL<br/>AND sentimentAnalysis = true
    DB-->>Repo: [user1, user2]
    Repo-->>Svc: [user1, user2]

    loop For each user
        Svc->>Svc: getEntriesFromLastSevenDays(user)<br/>Filter entries where date > now - 7 days

        alt Has entries with sentiment
            Svc->>Svc: findDominantSentiment(entries)<br/>Group by sentiment, find max count
            Svc->>Svc: buildSummaryMessage()<br/>"Hi user, dominant mood: HAPPY..."

            Svc->>Producer: sendWeeklySentiment({ email, sentiment })
            Producer->>Producer: Serialize to JSON
            Producer->>Kafka: send(topic, email_key, json_payload)

            Note over Kafka,SMTP: Async consumption
            Kafka->>Consumer: consume(message)
            Consumer->>Consumer: Deserialize JSON → SentimentData
            Consumer->>Email: sendEmail(to, subject, body)
            Email->>SMTP: SimpleMailMessage
        else No entries
            Svc->>Svc: Skip user (log debug)
        end
    end

    Svc-->>Cron: processed count
```

## 10. Weather with Redis Cache - Detailed Sequence

```mermaid
sequenceDiagram
    actor Client
    participant Ctrl as UserController
    participant Svc as WeatherService
    participant Redis as RedisService
    participant Cache as Redis
    participant AppCache as AppCache
    participant API as Weatherstack API

    Client->>Ctrl: GET /user?lat=28.6&lon=77.2
    Ctrl->>Svc: getWeather(28.6, 77.2)

    Svc->>Redis: get("weather_of_28.6_77.2", WeatherResponse.class)
    Redis->>Cache: GET weather_of_28.6_77.2
    
    alt Cache HIT
        Cache-->>Redis: JSON string
        Redis->>Redis: ObjectMapper.readValue → WeatherResponse
        Redis-->>Svc: WeatherResponse
        Svc-->>Ctrl: WeatherResponse (from cache)
    else Cache MISS
        Cache-->>Redis: null
        Redis-->>Svc: null

        Svc->>AppCache: get(WEATHER_API)
        AppCache-->>Svc: URL template with placeholders
        Svc->>Svc: Replace {lat}, {lon} and {apiKey} in URL

        Svc->>API: GET https://api.openweathermap.org/data/2.5/weather?...
        API-->>Svc: { main: { temp: 307, feels_like: 305 }, weather: [...] }

        Svc->>Redis: set("weather_of_28.6_77.2", response, 300)
        Redis->>Cache: SET weather_of_28.6_77.2 TTL=300s

        Svc-->>Ctrl: WeatherResponse (from API)
    end

    Ctrl->>Ctrl: Build greeting message
    Ctrl-->>Client: { data: "Hi johndoe, Weather: clear sky, Feels like: 32°C" }
```

## 11. Error Handling - Detailed Flow

```mermaid
sequenceDiagram
    actor Client
    participant Ctrl as Controller
    participant Svc as Service
    participant GEH as GlobalExceptionHandler

    Client->>Ctrl: Request

    alt Malformed JSON body
        Ctrl-->>GEH: HttpMessageNotReadableException
        GEH-->>Client: 400 { success: false, errors: [{ code: "ERR_6002" }] }
    else Validation fails (@Valid)
        Ctrl-->>GEH: MethodArgumentNotValidException
        GEH-->>Client: 400 { success: false, errors: [{ code: "ERR_6001", field: "userName" }] }
    else Duplicate username
        Ctrl->>Svc: saveNewUser()
        Svc-->>GEH: JournalAppException(USER_ALREADY_EXISTS)
        GEH-->>Client: 409 { success: false, errors: [{ code: "ERR_1002" }] }
    else User not found
        Ctrl->>Svc: findByUserName()
        Svc-->>GEH: UserNotFoundException
        GEH-->>Client: 404 { success: false, errors: [{ code: "ERR_1001" }] }
    else Entry not owned by user
        Ctrl-->>GEH: JournalEntryAccessDeniedException
        GEH-->>Client: 403 { success: false, errors: [{ code: "ERR_2002" }] }
    else Gemini AI down
        Svc->>Svc: analyseSentimentSafely catches exception
        Svc->>Svc: Falls back to existing sentiment
        Svc-->>Ctrl: Success (graceful degradation)
        Ctrl-->>Client: 201 { success: true, data: { sentiment: null } }
    else Unexpected error
        Svc-->>GEH: RuntimeException
        GEH-->>Client: 500 { success: false, errors: [{ code: "ERR_9001" }] }
    end
```

## 12. Unified API Response Structure

Every endpoint returns this envelope:

```json
{
    "success": true,
    "message": "Operation completed.",
    "data": { },
    "errors": [
        {
            "code": "ERR_XXXX",
            "message": "Human readable error",
            "field": "fieldName (for validation errors)"
        }
    ],
    "metadata": { }
}
```

- `success` → always present (true/false)
- `message` → human-readable summary
- `data` → payload (null on errors)
- `errors` → list of errors (null on success)
- `metadata` → reserved for pagination (empty for now)