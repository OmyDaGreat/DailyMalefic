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
**Purpose:** Quick health check.

**Response:** `200 OK` with `pong`.

**Notes:** Good for liveness checks.

### GET /health
**Purpose:** Health check for Docker.

**Response:** `200 OK` with `healthy`.

**Notes:** Handy for readiness/liveness probes.

### GET /entry
**Purpose:** Get journal entries.

**Request:** Optional query params:

- `id=<entry-id>` to load a single entry
- `date=YYYY-MM-DD` to load entries for a specific date

If both are provided, `id` takes precedence.

**Response:** `200 OK` returns:

- a single entry object for `?id=`
- an array of entries for no query parameters or `?date=`

**Errors:**

- `400 Bad Request` if `date` is invalid
- `404 Not Found` if the entry or date isn't found

**Notes:**

- No query parameters return the latest date's entries
- Results are sorted by `id` for no-query and date lookups

**Examples:**

No query parameters:

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

`?id=<entry-id>`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "author": "Author Name",
  "text": "Entry text",
  "date": "2026-05-11",
  "song": null
}
```

`?date=YYYY-MM-DD`:

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "author": "Author Name",
    "text": "Entry text",
    "date": "2026-05-11",
    "song": null
  }
]
```

### GET /entry/history
**Purpose:** Get all historical entries.

**Response:** `200 OK` with entries sorted by date ascending.

**Notes:** Latest date is last.

**Example:**

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
**Purpose:** Create or update an entry.

**Authentication:** Send `X-API-Key` when `API_KEY` is set.

**Request:**

- `Content-Type: application/json`
- Optional `X-API-Key: your-api-key`
- JSON body with `author`, `text`, `date`, optional `songQuery`, and optional `id`

**Request example: new entry**
```json
{
  "author": "New Author",
  "text": "Entry text",
  "date": "2026-05-11",
  "songQuery": "song name or artist (optional - will auto-search YouTube Music)"
}
```

**Request example: update existing entry by ID**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "author": "Updated Author",
  "text": "Updated entry text",
  "date": "2026-05-11",
  "songQuery": "different song (optional)"
}
```

**Response:** `200 OK` with the saved entry.

**Errors:** `401 Unauthorized` with `Invalid or missing API key` when the key is wrong or missing.

**Notes:**

- If `id` is provided, that entry gets updated or created
- If `id` is missing, a new one is generated
- `songQuery` is optional and searches YouTube Music for the first match
- Multiple entries can share the same date

**Response example:**

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

### DELETE /entry
**Purpose:** Delete an entry by ID.

**Authentication:** Send `X-API-Key` when `API_KEY` is set.

**Request:**

- `Content-Type: application/json`
- Optional `X-API-Key: your-api-key`
- JSON body containing `id`

**Request example:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**

- `200 OK` with `Entry deleted` when the entry is removed
- `404 Not Found` with `Entry not found, nothing deleted` when no matching entry exists

**Errors:** `401 Unauthorized` with `Invalid or missing API key` when the key is wrong or missing.

**Notes:** The entry is matched by `id`.

**Success example:**
```
HTTP 200 OK
Entry deleted
```

**Not found example:**
```
HTTP 404 Not Found
Entry not found, nothing deleted
```

## Configuration

### Environment Variables

- `API_KEY`: Optional API key for POST authentication. If set, all POST requests must include `X-API-Key` header. Otherwise, POST endpoints are open without authentication.
- `JAVA_OPTS`: Java runtime options (default: `-Xmx512m`)

## Docker

Use the included `docker-compose.yml`:

```bash
export API_KEY=your-secret-key   # optional
docker compose up --build
```

That starts the app on port `7290`, mounts `entry_data` to `/data`, and passes `API_KEY` through if you set it.

## Significant Libraries

- **HTTP Framework**: [http4k](https://www.http4k.org/)
- **Music API**: [syk-sh's ytm-kt](https://gitlab.com/syk.sh/ytm-kt)
