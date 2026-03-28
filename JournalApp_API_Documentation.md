# Journal App — API Documentation

Base URL: `http://localhost:8080`

All responses follow the unified envelope:
```json
{
    "success": true/false,
    "message": "...",
    "data": { ... },
    "errors": [{ "code": "ERR_XXXX", "message": "...", "field": "..." }],
    "metadata": { }
}
```

---

## 1. PUBLIC ENDPOINTS (No Authentication)

---

### 1.1 Health Check

| | |
|---|---|
| Method | `GET` |
| URL | `/public/health-check` |
| Auth | None |
| Description | Verify the service is running |

Response `200 OK`:
```json
{
    "success": true,
    "message": "Service is healthy.",
    "data": "OK"
}
```

---

### 1.2 Signup

| | |
|---|---|
| Method | `POST` |
| URL | `/public/signup` |
| Auth | None |
| Content-Type | `application/json` |
| Description | Register a new user account |

Request Body:
```json
{
    "userName": "johndoe",
    "password": "password123",
    "email": "johndoe@example.com",
    "sentimentAnalysis": true
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| userName | String | Yes | 3-50 chars, alphanumeric + underscore only |
| password | String | Yes | 8-100 chars |
| email | String | No | Valid email format |
| sentimentAnalysis | boolean | No | Default: false |

Response `201 Created`:
```json
{
    "success": true,
    "message": "User registered successfully.",
    "data": {
        "userId": "a1b2c3d4-...",
        "userName": "johndoe",
        "email": "johndoe@example.com",
        "sentimentAnalysis": true,
        "roles": ["USER"],
        "journalEntryCount": 0
    }
}
```

Error `409 Conflict` — Username already taken:
```json
{
    "success": false,
    "message": "A user with this username already exists.",
    "errors": [{
        "code": "ERR_1002",
        "message": "A user with this username already exists. | username: johndoe"
    }]
}
```

Error `400 Bad Request` — Validation failed:
```json
{
    "success": false,
    "message": "Request validation failed.",
    "errors": [
        { "code": "ERR_6001", "field": "userName", "message": "Username is required" },
        { "code": "ERR_6001", "field": "password", "message": "Password must be between 8 and 100 characters" }
    ]
}
```

---

### 1.3 Login

| | |
|---|---|
| Method | `POST` |
| URL | `/public/login` |
| Auth | None |
| Content-Type | `application/json` |
| Description | Authenticate and receive a JWT token |

Request Body:
```json
{
    "userName": "johndoe",
    "password": "password123"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| userName | String | Yes | Not blank |
| password | String | Yes | Not blank |

Response `200 OK`:
```json
{
    "success": true,
    "message": "Login successful.",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "tokenType": "Bearer",
        "userName": "johndoe",
        "roles": ["USER"],
        "expiresIn": 3600
    }
}
```

Error `401 Unauthorized` — Invalid credentials:
```json
{
    "success": false,
    "message": "Invalid username or password.",
    "errors": [{
        "code": "ERR_1006",
        "message": "Invalid username or password."
    }]
}
```

---

## 2. JOURNAL ENTRY ENDPOINTS (JWT Required — USER role)

All endpoints require header: `Authorization: Bearer <jwt_token>`

---

### 2.1 Create Journal Entry

| | |
|---|---|
| Method | `POST` |
| URL | `/journal` |
| Auth | Bearer JWT |
| Content-Type | `application/json` |
| Description | Create a new journal entry. Sentiment is auto-detected by Gemini AI if the user has sentimentAnalysis enabled and no sentiment is provided. |

Request Body:
```json
{
    "title": "Great day at the park",
    "content": "Enjoyed sunshine and fresh air. Feeling grateful for the little things."
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| title | String | Yes | 1-200 chars, not blank |
| content | String | No | Max 10000 chars |
| sentiment | String | No | HAPPY, SAD, ANGRY, or ANXIOUS. If omitted and user has sentimentAnalysis=true, Gemini AI auto-detects it. |

Response `201 Created`:
```json
{
    "success": true,
    "message": "Journal entry created.",
    "data": {
        "id": "f7e8d9c0-...",
        "title": "Great day at the park",
        "content": "Enjoyed sunshine and fresh air. Feeling grateful for the little things.",
        "date": "2026-03-29T10:15:30.000+00:00",
        "sentiment": "HAPPY",
        "authorUserName": "johndoe"
    }
}
```

Notes:
- If Gemini AI is unavailable, the entry is saved with `sentiment: null` (graceful degradation).
- If the user has `sentimentAnalysis: false`, Gemini is not called regardless.
- If `sentiment` is provided in the request body, Gemini is skipped.

---

### 2.2 Get All Journal Entries

| | |
|---|---|
| Method | `GET` |
| URL | `/journal` |
| Auth | Bearer JWT |
| Description | Returns all journal entries belonging to the authenticated user |

Response `200 OK`:
```json
{
    "success": true,
    "message": "Journal entries retrieved.",
    "data": [
        {
            "id": "f7e8d9c0-...",
            "title": "Great day at the park",
            "content": "Enjoyed sunshine...",
            "date": "2026-03-29T10:15:30.000+00:00",
            "sentiment": "HAPPY",
            "authorUserName": "johndoe"
        },
        {
            "id": "a1b2c3d4-...",
            "title": "Rough Monday",
            "content": "Everything went wrong...",
            "date": "2026-03-28T08:00:00.000+00:00",
            "sentiment": "ANGRY",
            "authorUserName": "johndoe"
        }
    ]
}
```

Response `200 OK` — No entries:
```json
{
    "success": true,
    "message": "No journal entries found.",
    "data": []
}
```

---

### 2.3 Get Journal Entry by ID

| | |
|---|---|
| Method | `GET` |
| URL | `/journal/id/{id}` |
| Auth | Bearer JWT |
| Description | Returns a single journal entry. The entry must belong to the authenticated user. |

| Path Param | Type | Description |
|------------|------|-------------|
| id | UUID | Journal entry ID |

Response `200 OK`:
```json
{
    "success": true,
    "data": {
        "id": "f7e8d9c0-...",
        "title": "Great day at the park",
        "content": "Enjoyed sunshine...",
        "date": "2026-03-29T10:15:30.000+00:00",
        "sentiment": "HAPPY",
        "authorUserName": "johndoe"
    }
}
```

Error `403 Forbidden` — Entry belongs to another user:
```json
{
    "success": false,
    "message": "You do not have access to this journal entry.",
    "errors": [{
        "code": "ERR_2002",
        "message": "You do not have access to this journal entry. | id: f7e8d9c0-..."
    }]
}
```

Error `404 Not Found` — Entry does not exist:
```json
{
    "success": false,
    "message": "Journal entry not found.",
    "errors": [{
        "code": "ERR_2001",
        "message": "Journal entry not found. | id: f7e8d9c0-..."
    }]
}
```

---

### 2.4 Update Journal Entry

| | |
|---|---|
| Method | `PUT` |
| URL | `/journal/id/{id}` |
| Auth | Bearer JWT |
| Content-Type | `application/json` |
| Description | Update title and/or content. Blank fields are ignored. Sentiment is re-analysed by Gemini if user has sentimentAnalysis enabled. |

| Path Param | Type | Description |
|------------|------|-------------|
| id | UUID | Journal entry ID |

Request Body (all fields optional):
```json
{
    "title": "Updated: Great day at the park",
    "content": "Reflecting on it, today was actually amazing."
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| title | String | No | Ignored if blank/whitespace |
| content | String | No | Ignored if blank/whitespace |
| sentiment | String | No | Currently unused by controller — sentiment is auto-detected |

Response `200 OK`:
```json
{
    "success": true,
    "message": "Journal entry updated.",
    "data": {
        "id": "f7e8d9c0-...",
        "title": "Updated: Great day at the park",
        "content": "Reflecting on it, today was actually amazing.",
        "date": "2026-03-29T10:15:30.000+00:00",
        "sentiment": "HAPPY",
        "authorUserName": "johndoe"
    }
}
```

Error `403 Forbidden` — Not owned by user (same as GET by ID)
Error `404 Not Found` — Entry does not exist (same as GET by ID)

---

### 2.5 Delete Journal Entry

| | |
|---|---|
| Method | `DELETE` |
| URL | `/journal/id/{id}` |
| Auth | Bearer JWT |
| Description | Permanently delete a journal entry. Must be owned by the authenticated user. |

| Path Param | Type | Description |
|------------|------|-------------|
| id | UUID | Journal entry ID |

Response `200 OK`:
```json
{
    "success": true,
    "message": "Journal entry deleted successfully."
}
```

Error `403 Forbidden` — Not owned by user (same as above)

---

## 3. USER ENDPOINTS (JWT Required — USER role)

All endpoints require header: `Authorization: Bearer <jwt_token>`

---

### 3.1 Weather Greeting

| | |
|---|---|
| Method | `GET` |
| URL | `/user?lat={lat}&lon={lon}` |
| Auth | Bearer JWT |
| Description | Returns a personalised greeting with live weather data for the given coordinates. Weather is cached in Redis for 5 minutes. Temperatures are converted from Kelvin to Celsius. |

| Query Param | Type | Required | Validation |
|-------------|------|----------|------------|
| lat | double | Yes | -90.0 to 90.0 |
| lon | double | Yes | -180.0 to 180.0 |

Response `200 OK`:
```json
{
    "success": true,
    "message": "Greeting retrieved.",
    "data": "Hi johndoe, Weather: clear sky, Feels like: 32.0°C"
}
```

Response `200 OK` — Weather API returned null:
```json
{
    "success": true,
    "message": "Greeting retrieved.",
    "data": "Hi johndoe"
}
```

Error `503 Service Unavailable` — Weather API down:
```json
{
    "success": false,
    "message": "Weather service is currently unavailable.",
    "errors": [{
        "code": "ERR_4001",
        "message": "Weather service is currently unavailable."
    }]
}
```

---

### 3.2 Update User Profile

| | |
|---|---|
| Method | `PUT` |
| URL | `/user` |
| Auth | Bearer JWT |
| Content-Type | `application/json` |
| Description | Update the authenticated user's profile. Only provided (non-blank) fields are updated. Password is re-encoded. Username uniqueness is checked if changed. |

Request Body (all fields optional):
```json
{
    "userName": "johndoe_v2",
    "password": "newSecurePass123",
    "email": "newemail@example.com",
    "sentimentAnalysis": true
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| userName | String | No | 3-50 chars if provided |
| password | String | No | 8-100 chars if provided |
| email | String | No | Valid email format |
| sentimentAnalysis | Boolean | No | true/false. Only updated if explicitly provided (not null). |

Response `200 OK`:
```json
{
    "success": true,
    "message": "User updated successfully.",
    "data": {
        "userId": "a1b2c3d4-...",
        "userName": "johndoe_v2",
        "email": "newemail@example.com",
        "sentimentAnalysis": true,
        "roles": ["USER"]
    }
}
```

Error `409 Conflict` — New username already taken:
```json
{
    "success": false,
    "message": "A user with this username already exists.",
    "errors": [{
        "code": "ERR_1002",
        "message": "A user with this username already exists. | username: johndoe_v2"
    }]
}
```

Note: Password is never returned in the response.

---

### 3.3 Delete User Account

| | |
|---|---|
| Method | `DELETE` |
| URL | `/user` |
| Auth | Bearer JWT |
| Description | Permanently delete the authenticated user's account and all associated data. |

Response `200 OK`:
```json
{
    "success": true,
    "message": "User deleted successfully."
}
```

---

## 4. ADMIN ENDPOINTS (JWT Required — ADMIN role)

All endpoints require header: `Authorization: Bearer <jwt_token>` with a user that has the ADMIN role.
Regular users receive `403 Forbidden`.

---

### 4.1 Get All Users

| | |
|---|---|
| Method | `GET` |
| URL | `/admin/all-users` |
| Auth | Bearer JWT (ADMIN) |
| Description | Returns all registered users |

Response `200 OK`:
```json
{
    "success": true,
    "message": "Users retrieved.",
    "data": [
        {
            "userId": "a1b2c3d4-...",
            "userName": "johndoe",
            "email": "johndoe@example.com",
            "sentimentAnalysis": true,
            "roles": ["USER"]
        },
        {
            "userId": "e5f6g7h8-...",
            "userName": "adminuser",
            "email": "admin@example.com",
            "sentimentAnalysis": false,
            "roles": ["USER", "ADMIN"]
        }
    ]
}
```

Error `404 Not Found` — No users in system:
```json
{
    "success": false,
    "message": "No users found."
}
```

---

### 4.2 Create Admin User

| | |
|---|---|
| Method | `POST` |
| URL | `/admin/create-admin-user` |
| Auth | Bearer JWT (ADMIN) |
| Content-Type | `application/json` |
| Description | Create a new user with both USER and ADMIN roles |

Request Body:
```json
{
    "userName": "newadmin",
    "password": "adminpass123",
    "email": "newadmin@example.com"
}
```

Response `200 OK`:
```json
{
    "success": true,
    "message": "Admin user created.",
    "data": {
        "userId": "...",
        "userName": "newadmin",
        "email": "newadmin@example.com",
        "roles": ["USER", "ADMIN"]
    }
}
```

Error `409 Conflict` — Username already exists (same as signup)

---

### 4.3 Clear App Cache

| | |
|---|---|
| Method | `GET` |
| URL | `/admin/clear-app-cache` |
| Auth | Bearer JWT (ADMIN) |
| Description | Force refresh the in-memory config cache (AppCache). Reloads all config entries from the database. |

Response `200 OK`:
```json
{
    "success": true,
    "message": "App cache refreshed successfully."
}
```

---

### 4.4 Trigger Weekly Sentiment Report

| | |
|---|---|
| Method | `POST` |
| URL | `/admin/trigger-weekly-sentiment` |
| Auth | Bearer JWT (ADMIN) |
| Description | Manually trigger the weekly sentiment report. Normally runs automatically every Sunday at 9 AM. Processes all users with sentimentAnalysis=true and a valid email. |

Response `200 OK`:
```json
{
    "success": true,
    "message": "Weekly sentiment report triggered. Processed 3 users."
}
```

---

### 4.5 Get All Configs

| | |
|---|---|
| Method | `GET` |
| URL | `/admin/configs` |
| Auth | Bearer JWT (ADMIN) |
| Description | List all application config entries stored in the database (e.g. API URLs for Weather, Gemini). |

Response `200 OK`:
```json
{
    "success": true,
    "message": "Configs retrieved.",
    "data": [
        {
            "id": "c1d2e3f4-...",
            "key": "WEATHER_API",
            "value": "https://api.openweathermap.org/data/2.5/weather?lat=<lat>&lon=<lon>&appid=<apiKey>"
        },
        {
            "id": "g5h6i7j8-...",
            "key": "GEMINI_API",
            "value": "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
        }
    ]
}
```

Error `404 Not Found` — No configs:
```json
{
    "success": false,
    "message": "No configs found."
}
```

---

### 4.6 Update Config by Key

| | |
|---|---|
| Method | `PUT` |
| URL | `/admin/configs/{key}` |
| Auth | Bearer JWT (ADMIN) |
| Content-Type | `application/json` |
| Description | Update the value of an existing config entry. AppCache is refreshed immediately after the update so the new value takes effect without waiting for the 10-minute scheduler. |

| Path Param | Type | Description |
|------------|------|-------------|
| key | String | Config key (e.g. `WEATHER_API`, `GEMINI_API`) |

Request Body:
```json
{
    "value": "https://api.openweathermap.org/data/2.5/weather?lat=<lat>&lon=<lon>&appid=<apiKey>"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| value | String | Yes | Not blank, max 255 chars |

Response `200 OK`:
```json
{
    "success": true,
    "message": "Config updated and cache refreshed.",
    "data": {
        "id": "c1d2e3f4-...",
        "key": "WEATHER_API",
        "value": "https://api.openweathermap.org/data/2.5/weather?lat=<lat>&lon=<lon>&appid=<apiKey>"
    }
}
```

Error `500 Internal Server Error` — Config key not found:
```json
{
    "success": false,
    "message": "An unexpected internal error occurred.",
    "errors": [{
        "code": "ERR_9001",
        "message": "An unexpected internal error occurred. | Config key not found: INVALID_KEY"
    }]
}
```

---

### 4.7 Update User by Username (Admin)

| | |
|---|---|
| Method | `PUT` |
| URL | `/admin/users/{username}` |
| Auth | Bearer JWT (ADMIN) |
| Content-Type | `application/json` |
| Description | Update any user's profile by their username. Same update logic as the user self-update endpoint. |

| Path Param | Type | Description |
|------------|------|-------------|
| username | String | Target user's username |

Request Body (all fields optional):
```json
{
    "userName": "johndoe_renamed",
    "password": "newPassword123",
    "email": "updated@example.com",
    "sentimentAnalysis": true
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| userName | String | No | 3-50 chars if provided |
| password | String | No | 8-100 chars if provided |
| email | String | No | Valid email format |
| sentimentAnalysis | Boolean | No | Only updated if non-null |

Response `200 OK`:
```json
{
    "success": true,
    "message": "User updated successfully.",
    "data": {
        "userId": "a1b2c3d4-...",
        "userName": "johndoe_renamed",
        "email": "updated@example.com",
        "sentimentAnalysis": true,
        "roles": ["USER"]
    }
}
```

Error `404 Not Found` — User does not exist:
```json
{
    "success": false,
    "message": "User not found.",
    "errors": [{
        "code": "ERR_1001",
        "message": "User not found. | username: nonexistent"
    }]
}
```

Error `409 Conflict` — New username already taken (same as other update endpoints)

---

### 4.8 Get User's Journal Entries (Admin)

| | |
|---|---|
| Method | `GET` |
| URL | `/admin/users/{username}/journals` |
| Auth | Bearer JWT (ADMIN) |
| Description | View all journal entries for a specific user. Useful for admin oversight and support. |

| Path Param | Type | Description |
|------------|------|-------------|
| username | String | Target user's username |

Response `200 OK`:
```json
{
    "success": true,
    "message": "Journal entries retrieved.",
    "data": [
        {
            "id": "f7e8d9c0-...",
            "title": "Great day at the park",
            "content": "Enjoyed sunshine...",
            "date": "2026-03-29T10:15:30.000+00:00",
            "sentiment": "HAPPY",
            "authorUserName": "johndoe"
        }
    ]
}
```

Response `200 OK` — No entries:
```json
{
    "success": true,
    "message": "No journal entries found.",
    "data": []
}
```

Error `404 Not Found` — User does not exist (same as above)

---

## 5. COMMON ERROR RESPONSES

These errors can occur on any authenticated endpoint.

---

### 5.1 Missing or Invalid JWT Token (401)

Occurs when no token is provided, or the token is malformed/expired.

```json
{
    "success": false,
    "message": "Invalid username or password.",
    "errors": [{
        "code": "ERR_1006",
        "message": "Invalid username or password."
    }]
}
```

Note: Spring Security returns 401 before reaching the controller. The response format depends on the security configuration.

---

### 5.2 Insufficient Permissions (403)

Occurs when a USER-role token tries to access an ADMIN endpoint.

```json
{
    "success": false,
    "message": "You do not have permission to perform this action.",
    "errors": [{
        "code": "ERR_5003",
        "message": "You do not have permission to perform this action."
    }]
}
```

---

### 5.3 Malformed Request Body (400)

Occurs when the JSON body is unparseable (missing quotes, trailing commas, etc.).

```json
{
    "success": false,
    "message": "Request body is missing or malformed.",
    "errors": [{
        "code": "ERR_6002",
        "message": "Request body is missing or malformed."
    }]
}
```

---

### 5.4 Validation Failed (400)

Occurs when @Valid annotations fail on request DTOs.

```json
{
    "success": false,
    "message": "Request validation failed.",
    "errors": [
        { "code": "ERR_6001", "field": "userName", "message": "Username is required" },
        { "code": "ERR_6001", "field": "password", "message": "Password must be between 8 and 100 characters" },
        { "code": "ERR_6001", "field": "email", "message": "Please provide a valid email address" }
    ]
}
```

---

### 5.5 Internal Server Error (500)

Catch-all for unexpected exceptions.

```json
{
    "success": false,
    "message": "An unexpected internal error occurred.",
    "errors": [{
        "code": "ERR_9001",
        "message": "An unexpected internal error occurred."
    }]
}
```

---

## 6. ERROR CODE REFERENCE

| Code | HTTP Status | Description |
|------|-------------|-------------|
| ERR_1001 | 404 | User not found |
| ERR_1002 | 409 | Username already exists |
| ERR_1003 | 500 | User creation failed |
| ERR_1004 | 500 | User update failed |
| ERR_1005 | 500 | User deletion failed |
| ERR_1006 | 401 | Invalid credentials |
| ERR_2001 | 404 | Journal entry not found |
| ERR_2002 | 403 | Journal entry access denied |
| ERR_2003 | 500 | Journal entry creation failed |
| ERR_2004 | 500 | Journal entry update failed |
| ERR_2005 | 500 | Journal entry deletion failed |
| ERR_3001 | 500 | Sentiment analysis failed (empty Gemini response) |
| ERR_3002 | 400 | Invalid sentiment value from Gemini |
| ERR_4001 | 503 | Weather service unavailable |
| ERR_4002 | 503 | Gemini AI service unavailable |
| ERR_4003 | 500 | Email send failed |
| ERR_4004 | 500 | Kafka publish failed |
| ERR_5001 | 401 | JWT token invalid or malformed |
| ERR_5002 | 401 | JWT token expired |
| ERR_5003 | 403 | Access denied (insufficient role) |
| ERR_6001 | 400 | Request validation failed |
| ERR_6002 | 400 | Request body missing or malformed |
| ERR_9001 | 500 | Unexpected internal error |

---

## 7. AUTHENTICATION

All protected endpoints require the `Authorization` header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

- Algorithm: HS256
- Expiry: 1 hour (3600 seconds)
- Subject claim: username
- Validation: checks both username match AND expiry

To obtain a token, call `POST /public/login`.

---

## 8. BACKGROUND JOBS

These are not API endpoints but automated processes:

| Schedule | Job | Description |
|----------|-----|-------------|
| Every Sunday 9:00 AM | Weekly Sentiment Report | Aggregates last 7 days of entries per user, finds dominant sentiment, sends email via Kafka (or direct SMTP fallback) |
| Every 10 minutes | App Cache Refresh | Reloads config entries from database into in-memory cache |

The weekly sentiment report can also be triggered manually via `POST /admin/trigger-weekly-sentiment`.

---

## 9. SWAGGER UI

Interactive API documentation is available at:

```
http://localhost:8080/docs
```

Swagger UI includes a global "Authorize" button for JWT Bearer tokens. Click it, paste your token, and all subsequent requests will include the Authorization header automatically.