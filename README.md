# DailyMalefic

A lightweight HTTP service for managing daily quotes with history tracking and API key authentication.

## Features

- **Quote Management**: Store and retrieve daily quotes via REST API
- **Quote History**: Track all historical quotes with timestamps
- **API Key Authentication**: Secure POST endpoints with API key authentication
- **CORS Support**: Pre-configured CORS policies for web clients
- **Health Checks**: Built-in health endpoint for monitoring
- **Persistence**: File-based storage with automatic initialization
- **Docker Support**: Multi-platform Docker images (amd64/arm64)

## Endpoints

### GET /ping
Health check endpoint returning "pong"

### GET /health
Health check endpoint returning "healthy" (used by Docker healthcheck)

### GET /quote
Retrieve the current daily quote
```json
{
  "author": "Author Name",
  "text": "Quote text"
}
```

### GET /quote/history
Retrieve all historical quotes with timestamps
```json
[
  {
    "author": "Author Name",
    "text": "Quote text",
    "timestamp": "2026-04-05T01:30:00Z"
  }
]
```

### POST /quote
Update the current quote (requires API key authentication)

**Headers:**
- `Content-Type: application/json`
- `X-API-Key: your-api-key` (required if API_KEY env var is set)

**Body:**
```json
{
  "author": "New Author",
  "text": "New quote text"
}
```

## Configuration

### Environment Variables

- `API_KEY`: Optional API key for POST authentication. If set, all POST requests must include `X-API-Key` header
- `JAVA_OPTS`: Java runtime options (default: `-Xmx512m`)

## Build

```bash
./gradlew distZip
```

## Run Locally

```bash
./gradlew run
```

Server starts on port 7290.

## Docker

### Build
```bash
docker build -t dailymalefic .
```

### Run
```bash
docker run -p 7290:7290 -v quote_data:/data -e API_KEY=your-secret-key dailymalefic
```

### Docker Compose
```bash
export API_KEY=your-secret-key
docker-compose up
```

## Development

### Run Tests
```bash
./gradlew test
```

### Dependency Management
This project uses Gradle version catalogs (`gradle/libs.versions.toml`) for centralized dependency management. All dependencies and versions are defined in the catalog for easy maintenance and updates.

### Project Structure
- `src/main/kotlin/xyz/malefic/daily/`
  - `DailyMalefic.kt` - Main application and HTTP routing
  - `format/Quote.kt` - Quote data model
  - `format/QuoteHistory.kt` - Quote history model
  - `storage/QuoteStorage.kt` - File-based persistence layer
- `gradle/libs.versions.toml` - Version catalog for dependency management

## Technical Stack

- **Language**: Kotlin 2.3.20
- **HTTP Framework**: http4k 6.40.0.0
- **Server**: Undertow
- **JSON**: Jackson with Java Time support
- **Testing**: JUnit Jupiter, Kotest, Hamkrest
- **Build**: Gradle with Kotlin DSL
- **Runtime**: JVM 23

