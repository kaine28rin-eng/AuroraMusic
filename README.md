# Aurora Music

Local-music Android player (Kotlin, Jetpack Compose, Media3/ExoPlayer) inspired by Spotify's discovery shelves and Apple Music's clean library.

## Build the APK (no Android Studio needed)
1. Create a GitHub repo and push this folder to the `main` branch.
2. Open **Actions > Build APK**; it runs automatically on push (or click *Run workflow*).
3. Download the `AuroraMusic-apk` artifact, unzip, and sideload `app-release.apk`.

## Build locally
Open the folder in Android Studio (Ladybug or newer) and press Run, or with JDK 17 and Gradle 8.10+:
`gradle assembleRelease` -> `app/build/outputs/apk/release/`.

## Features
- Library scan via MediaStore: Songs (sortable), Albums, Artists, Playlists, Liked songs
- Home shelves: Recently played, Most played, Recently added, Favorites, Shuffle all
- Search across songs, albums, artists
- Background playback with media notification, lock-screen and Bluetooth controls, gapless, audio focus, pause on unplug
- Now Playing with album-art-tinted background, seek, shuffle, repeat, queue, sleep timer
- Equalizer: per-band sliders, device presets, bass boost, virtualizer, loudness gain; settings persist

## Not yet built (roadmap)
Crossfade, synced lyrics (.lrc), tag editor, folder browsing, Android Auto browse tree, home-screen widget, Room DB for large libraries, last-queue restore, release keystore + Play Store setup.
