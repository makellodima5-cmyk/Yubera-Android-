# News source coverage and reuse review

**Reviewed:** 2026-11-04  
**Purpose:** identify a lawful, maintainable way to improve beyond the current single-source Shikimori feed. This is a technical/source review, not legal advice or permission to republish content.

## Current source: Shikimori

The Android integration currently requests one page of up to 30 topics from the public Shikimori news endpoint. The app uses the fields currently returned by that endpoint: title, short body preview, creation date, comment count, author and linked anime when available.

This does not establish broad coverage of anime-industry news. The current API response also does not provide a guaranteed full article body. The app therefore keeps source attribution and opens the original publication for full reading.

## Candidate 1: Anime News Network (ANN)

Official ANN pages confirm that ANN provides RSS and Atom feeds for all articles and multiple categories, including Newsroom, News, Press Release, Features and Interviews.

ANN also documents a separate Newsfeed program for displaying headlines on websites. Its help page asks consumers to fetch at roughly hourly intervals and to display the attribution link “News provided by AnimeNewsNetwork.com”. ANN's FAQ says that entirely reprinting an ANN story requires contacting its editors; it points users to the copyright statement for further reuse rules.

**Assessment:** strongest candidate for a broader editorial news source because the publisher explicitly maintains RSS/Atom feeds and a headline syndication program. However, the published Newsfeed instructions describe websites, not native Android apps. Do not assume that they grant YUBERA permission for use inside a redistributable APK. Seek written confirmation for the intended Android-app use and verify the current terms before implementation.

Official references:
- https://my.worldacg.com/newsfeed/
- https://my.worldacg.com/newsfeed/help.php
- https://my.worldacg.com/faq

## Candidate 2: Crunchyroll News

Crunchyroll's official News About page advertises a Crunchyroll News RSS feed. Its Terms of Use reserve rights in service content and restrict copying, redistribution and integration/indexing of a substantial part of content without explicit written permission.

**Assessment:** useful as a source to monitor, but do not add it as a YUBERA feed source until written permission for the intended app use is confirmed. The existence of an RSS endpoint alone is not a blanket redistribution licence.

Official references:
- https://www.crunchyroll.com/hi/news/about
- https://www.crunchyroll.com/de/tos

## Recommendation

1. Keep Shikimori as the currently implemented source and preserve its explicit attribution.
2. Contact ANN first about whether its RSS/Newsfeed content may be displayed in a native Android application, with source links and attribution, and what fields/excerpts/images are allowed.
3. Do not integrate Crunchyroll content without explicit written authorization.
4. Until permission is confirmed, do not scrape publisher pages, copy full article text, rehost images or imply that RSS availability means reuse rights.
5. Once a source is approved, use a source adapter and store source identity plus the publisher's stable external item ID. Deduplicate on (source, external ID), preserve publication time and source URL, and keep attribution visible.
6. Treat categories inferred from headlines as convenience labels, not publisher-defined classifications. Preserve an “other/industry” fallback instead of forcing every article into a confident category.

## Product constraints

- The current single-source feed must not be described as a complete anime-industry news overview.
- Full text should remain on the publisher's website unless the publisher explicitly grants republishing rights.
- Short excerpts, author bylines, thumbnails and translations also require source-specific review; do not assume all RSS fields may be cached or redistributed.
- Any future source integration needs failure handling, item de-duplication, refresh throttling, a source label and a canonical external link.
