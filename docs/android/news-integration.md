# Android news feed

## Source and limits

The first Android news feed reads the public Shikimori news forum through its topics API. It requests one page of up to 30 news topics when the news screen is opened; it does not currently crawl the full forum or the wider anime industry.

Each card links back to the original Shikimori publication and shows source attribution. The UI explicitly describes this as one source, not a complete view of the anime industry.

The API response currently supplies a topic preview, not a verified full article body. The detail screen shows the available preview and opens the full publication on Shikimori. It does not pretend that a short excerpt is the complete article.

## Categories and search

The source API does not provide YUBERA's editorial category taxonomy. Categories are therefore assigned locally using simple keyword rules over each topic title and short body preview:

- Announcements
- New seasons
- Industry (fallback category)
- Events
- Interviews

These are convenience filters, not authoritative editorial labels. A topic can be classified imperfectly.

Local search matches title, preview text, linked anime title and author name. Category and search filters also apply when the user opens the bookmarks list.

## Bookmarks and local cache

- Room stores the last non-empty successful response.
- A failed request or an empty response does not erase the previous cache.
- A successful refresh updates the current 30-item feed but preserves bookmarked article records even when they fall outside the current page.
- Bookmarked records remain available to the detail screen and can be found through the dedicated bookmarks filter.
- Removing a bookmark removes its saved marker. The now-unreferenced old article row is pruned during a subsequent successful refresh.
- Bookmarks are currently local to this Android installation. They are not synchronized with the website or another device.

## Content handling and sharing

- Article body is displayed as a plain-text preview; untrusted HTML is not rendered inside the app.
- The detail screen preserves the source link, attribution and related-anime label when available.
- Sharing sends the article title and source URL through Android's standard share sheet.
- Full articles open on Shikimori's own website.
- The feed is separate from personal episode notifications and does not subscribe users to global catalog updates.
