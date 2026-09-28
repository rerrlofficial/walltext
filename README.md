# WallText

Native Android Material 3 wallpaper editor.

## Current features
- Lock-screen wallpaper rendering using `WallpaperManager.FLAG_LOCK` (text is baked into the image; no lock-screen widget required).
- Wallpaper selection from device storage.
- Up to 5 placeholders.
- Placeholder types: text, table, checklist, bullets.
- Static mode or dynamic mode with up to 10 entries per placeholder.
- Drag-to-position editor preview.
- Font size and opacity controls.
- Last 10 configurations stored locally with DataStore.
- Dynamic rotation via WorkManager (15/30/60 minute intervals).
- Supabase SQL schema included for wallpaper catalog and optional cloud config sync.

## Open
Import the `WallText` folder into Android Studio. Build with JDK 17 and an Android SDK that includes API 35.

## Backend
Create a Supabase project and run `supabase/migrations/001_init.sql`. Add the Supabase URL/key through a secure configuration layer before enabling cloud sync.
