# Bedrock Builder

Offline Minecraft Bedrock structure editor for Android (Kotlin, Jetpack Compose, Material 3). No internet permission.

## Build the APK
**Android Studio:** open this folder, let Gradle sync (JDK 17, Android SDK 35), then *Build > Build APK(s)*
or run `gradle assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
**No local setup:** push to GitHub; the *Build APK* workflow (`.github/workflows/build-apk.yml`) uploads the APK as an artifact.
Install: copy the APK to the phone, open it, allow "install unknown apps".

## Using it
- **Home:** New structure (name + starting grid size), Import `.mcstructure`, per-item Export / Share / Duplicate / Rename / Delete.
  Opening a `.mcstructure` from a file manager also imports it.
- **Tools:** Place / Destroy / Replace / Pick (eyedropper) × Single, Box, Sphere, Dome, Cylinder, Pyramid, Line, Flood fill; *Hollow* option.
  Two-point shapes: tap first point, tap second point (sphere/dome radius = distance; cylinder = radius + height).
- **Grid:** yellow plane = active layer (▲/▼); *Slice* hides everything above it. Building outside the grid auto-expands it
  (setting), or use menu > Grid size. Drag = rotate, pinch = zoom, two-finger drag = pan. **Lock** freezes rotation/pan (zoom still works)
  and lets you drag-paint with Single. View menu has Top/Front/Side/Isometric presets.
- **Blocks:** ~900 vanilla ids, search + categories, quick state pickers (stairs facing, slab half, log axis, ...),
  raw `key=value` states, and **custom identifiers** (e.g. `my_pack:ruby_block`).
- **Saving:** autosaves as real `.mcstructure` files in app storage. Export writes the same file anywhere you choose.
  Load in game via a Structure Block (Import) or place it in a behavior pack's `structures/` folder.

## Notes
- Blocks are drawn as coloured shapes (no textures are bundled). Stairs/slabs/fences etc. use simplified shapes.
- Block ids follow Bedrock 1.21 naming written from memory; if the game reports an unknown block, use *Custom block* with the right id.
- Exported palette `version` is 18153475 (1.21.0.3); imported blocks keep their original version so the game can upgrade old files.
- Block entities (chest contents), waterlogging layer and entities in imported files are preserved; they are dropped if you replace/remove that block.

## Dev
`tools/selftest` has a JVM self-test for the NBT / structure / shapes / picking logic (`app/.../core` is pure Kotlin) and
`verify_mcstructure.py`, an independent parser used to check exported files.
