# DailyMalefic

A lightweight REST API service for managing daily journal entries with history tracking, music integration, and API key authentication.

## Features

- **Entry Management**: Store and retrieve daily journal entries via REST API
- **Music Integration**: Automatically search and associate YouTube Music songs with entries
- **Entry History**: Track all historical entries with dates
- **API Key Authentication**: Secure POST endpoints with API key authentication
- **Persistence**: File-based storage with automatic initialization

## Docker

Use the included `docker-compose.yml`:

```bash
export API_KEY=your-secret-key   # optional
docker compose up --build
```

Or pull and run the `maleficmarauder/dailymalefic` image directly from Docker Hub:

```bash
docker pull maleficmarauder/dailymalefic
docker run -p 7290:7290 -e API_KEY=your-secret-key -v entry_data:/data maleficmarauder/dailymalefic
```

These both start the app on port `7290`, mount `entry_data` to `/data`, and pass `API_KEY` through if you set it.

## Endpoints

### GET /ping
**Purpose:** Quick health check.

**Response:** `200 OK` with `pong`.

**Notes:** Good for liveness checks.

### GET /auth-ping
**Purpose:** Check if API key authentication is working/correct.

**Response:** `200 OK` returns if the provided `X-API-Key` is valid when `API_KEY` is set.

**Errors:** `401 Unauthorized` with `Invalid API key` when the API key is wrong.

**Notes:** Only relevant if `API_KEY` is set. Otherwise, this endpoint is open and always returns `200 OK`.

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
- `204 No Content` if the entry or date isn't found

**Notes:**

- No query parameters return the latest date's entries
- Results are sorted by `id` for no-query and date lookups

**Examples:**

No query parameters:

```json
[
  {
    "id": "1",
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
  "id": "1",
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
    "id": "1",
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
    "id": "1",
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
    "id": "2",
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
  "id": "1",
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
  "id": "1",
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
  "id": "1"
}
```

**Response:**

- `200 OK` with `Entry deleted` when the entry is removed
- `204 No Content` with `Entry not found, nothing deleted` when no matching entry exists

**Errors:** 

- `401 Unauthorized` with `Invalid or missing API key` when the key is wrong or missing
- `400 Bad Request` if `id` is missing from the request body or the `id` is invalid

**Notes:** The entry is matched by `id`.

**Success example:**
```
HTTP 200 OK
Entry deleted
```

**Not found example:**
```
HTTP 204 No Content
Entry not found, nothing deleted
```

## Configuration

### Environment Variables

- `API_KEY`: Optional API key for POST authentication. If set, all POST requests must include `X-API-Key` header. Otherwise, POST endpoints are open without authentication.
- `JAVA_OPTS`: Java runtime options (default: `-Xmx512m`)

## Significant Libraries

- **HTTP Framework**: [http4k](https://www.http4k.org/)
- **Music API**: [syk-sh's ytm-kt](https://gitlab.com/syk.sh/ytm-kt)
