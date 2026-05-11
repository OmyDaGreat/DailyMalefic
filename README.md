# DailyMalefic

A lightweight HTTP service for managing daily journal entries with history tracking, music integration, and API key authentication.

## Features

- **Entry Management**: Store and retrieve daily journal entries via REST API
- **Music Integration**: Automatically search and associate YouTube Music songs with entries
- **Entry History**: Track all historical entries with dates
- **API Key Authentication**: Secure POST endpoints with API key authentication
- **Persistence**: File-based storage with automatic initialization

## Endpoints

### GET /ping
Health check endpoint returning "pong"

### GET /health
Health check endpoint returning "healthy" (used by Docker healthcheck)

### GET /entry
Retrieve all entries from the most recent date (multiple entries can exist for the same date)
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "author": "Author Name",
    "text": "Entry text",
    "date": "2026-05-11",
    "song": {
      "id": "song-id",
      "name": "Song Name",
      "artists": [
        {
          "id": "artist-id",
          "name": "Artist Name"
        }
      ]
    }
  }
]
```

### GET /entry/history
Retrieve all historical entries sorted by date (ascending, so latest date is last)
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "author": "Author Name",
    "text": "Entry text",
    "date": "2026-05-09",
    "song": {
      "id": "song-id",
      "name": "Song Name",
      "artists": [
        {
          "id": "artist-id",
          "name": "Artist Name"
        }
      ]
    }
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "author": "Author Name",
    "text": "Entry text",
    "date": "2026-05-11",
    "song": null
  }
]
```

### POST /entry
Create a new entry or update an existing entry (requires API key authentication)

**Headers:**
- `Content-Type: application/json`
- `X-API-Key: your-api-key` (required if API_KEY env var is set)

**Request Body (new entry):**
```json
{
  "author": "New Author",
  "text": "Entry text",
  "date": "2026-05-11",
  "songQuery": "song name or artist (optional - will auto-search YouTube Music)"
}
```

**Request Body (update existing entry by ID):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "author": "Updated Author",
  "text": "Updated entry text",
  "date": "2026-05-11",
  "songQuery": "different song (optional)"
}
```

**Response:**
Returns the created or updated entry with its unique ID:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "author": "New Author",
  "text": "Entry text",
  "date": "2026-05-11",
  "song": {
    "id": "song-id",
    "name": "Song Name",
    "artists": [
      {
        "id": "artist-id",
        "name": "Artist Name"
      }
    ]
  }
}
```

**Notes:**
- If you provide an `id` in the request body, the existing entry with that ID will be updated or a new entry with that ID will be created.
- If you don't provide an `id`, a new unique ID will be generated automatically.
- The `songQuery` field is optional. When provided, the service will search YouTube Music for the song and automatically populate the `song` field with the first matching result. For better search results, include both the artist and song name.
- Multiple entries can exist with the same date. Each entry has a unique `id` for future updates.

### DELETE /entry
Delete an existing entry by ID (requires API key authentication)

**Headers:**
- `Content-Type: application/json`
- `X-API-Key: your-api-key` (required if API_KEY env var is set)

**Request Body:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (success):**
```
HTTP 200 OK
Entry deleted
```

**Response (not found):**
```
HTTP 404 Not Found
Entry not found, nothing deleted
```

## Configuration

### Environment Variables

- `API_KEY`: Optional API key for POST authentication. If set, all POST requests must include `X-API-Key` header. Otherwise, POST endpoints are open without authentication.
- `JAVA_OPTS`: Java runtime options (default: `-Xmx512m`)

## Build

```bash
./gradlew build
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
docker run -p 7290:7290 -v entry_data:/data -e API_KEY=your-secret-key dailymalefic
```

### Docker Compose
```bash
export API_KEY=your-secret-key
docker-compose up
```

## Significant Libraries

- **HTTP Framework**: [http4k](https://www.http4k.org/)
- **Music API**: [syk-sh's ytm-kt](https://gitlab.com/syk.sh/ytm-kt)
