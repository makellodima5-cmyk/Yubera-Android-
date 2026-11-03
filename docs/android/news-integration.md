# Android news feed

## Source and limits

The first Android news feed reads the public Shikimori news forum through its topics API. It requests one page of up to 30 news topics and sends the application User-Agent `YUBERA Android`. Requests happen when the news screen is opened; no high-frequency polling is used.

Each card links back to the original Shikimori publication and shows source attribution. The UI explicitly describes this as one source, not a complete view of the anime industry.

## Categories

The source API does not provide YUBERA's editorial category taxonomy. Categories are therefore assigned locally using simple keyword rules over each topic title and short body preview:

- Announcements
- New seasons
- Industry (fallback category)
- Events
- Interviews

These are convenience filters, not authoritative editorial labels. A topic can be classified imperfectly.

## Local cache and safety

- Room stores the last non-empty successful response.
- A failed request leaves the previous cache intact.
- An empty response does not erase the previous cache.
- Article body is displayed as plain text preview; untrusted HTML is not rendered inside the app.
- Full articles open on Shikimori's own website.
- The feed is separate from personal episode notifications and does not subscribe users to global catalog updates.
