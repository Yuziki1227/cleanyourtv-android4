# Android 4.0 (API 14) launcher

This fork lowers the launcher's `minSdk` from 26 to **14**, so it installs on Android 4.0/4.0.3/4.0.4
(Ice Cream Sandwich) TVs and boxes. It is the same launcher, not a second app: the Home screen, dock,
top shelf, app grid, app library, inputs menu, themes, settings and wallpapers are all here. Features
that need newer platform APIs are switched off or fall back to their older equivalents.

Upstream treats a lower `minSdk` as a fork; the code and this note belong to this fork, not to the
upstream review path in `AGENTS.md`.

## Build

You need JDK 17, Android SDK platform 34 and build-tools 34.0.0.

```sh
cd launcher
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk  (about 850 KB)
```

The release build is signed with the debug certificate, as upstream's is, so it installs over a debug
build. The APK is a legacy-multidex build (`classes.dex` + `classes2.dex`) for Dalvik.

What the build does differently from upstream:

| Setting | Value | Why |
|---|---|---|
| `minSdk` | 14 | Android 4.0 |
| `targetSdk` | 34 | unchanged; newer sets keep the same behaviour |
| `multiDexEnabled` | true | Dalvik has the 64K-method limit |
| `coreLibraryDesugaring` | `desugar_jdk_libs:2.1.5` | supplies `java.time`, `java.util.function` and the Java 8 collection methods that are API 24-26 on the platform |
| application class | `androidx.multidex.MultiDexApplication` | loads `classes2.dex` on API < 21 |
| `android.useFullClasspathForDexingTransform` | true | required by AGP 8.3+ when `minSdk` is below 24 |

`./gradlew test` runs the 274 JVM tests and passes with this configuration.

## Install on a 4.0 TV

`launcher/build-and-install-legacy.sh` uses only commands Android 4.0 has (`pm`/`am`/`dumpsys`; no
`cmd package`, no `settings`):

```sh
TV_HOST=192.168.1.50 ./launcher/build-and-install-legacy.sh
# or, to disable the old launcher after the new one starts:
TV_HOST=192.168.1.50 STOCK_LAUNCHER=com.example.tvlauncher ./launcher/build-and-install-legacy.sh
```

Without `STOCK_LAUNCHER`, the launcher is installed and opened; Android asks which Home to use, or
keeps the old one until you pick CleanYourTV once. With it, the old Home is disabled after the new one
has started, and the script puts it back if the new Home does not come to the front.

By hand, from the repo root:

```sh
adb connect <tv ip>:5555
adb install -r launcher/app/build/outputs/apk/release/app-release.apk
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME \
  -n com.gathin.tvhome/.MainActivity
# optional, once the launcher is on screen:
adb shell pm disable-user <stock launcher package>   # no --user 0 on API < 24
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
```

Turn USB/Network debugging off again when you are done.

## What works on Android 4.0

- The Home screen: dock, app grid, app library, focus highlight, top shelf, greeting, clock, date,
  lunar/Shamsi lines, stats and the clock screensaver started by Home itself (`ClockActivity`).
- Themes, `config.json`, wallpaper rotation and moods, font config, hidden apps, custom tile order.
- The Inputs menu: the configured `input_sources` / `input_action` vendor path, a configured source
  panel (`source_panel_*`) and the Live TV row. TV apps are found with the normal `LAUNCHER` category,
  so the app grid and app library are populated.
- The network pill: `NetworkInfo`/`WifiManager` report Wi-Fi, Ethernet, VPN and the SSID. There is no
  "validated" (captive-portal) signal before API 23; a connected network counts as online, so
  `wifi_status_warnings` cannot show the orange "no internet" state.
- Accessibility helpers (`HomeKeyService`, `InputKeyService`) can be enabled; `WRITE_SECURE_SETTINGS`
  is granted at install where the firmware allows it.

## What is gated off or degraded

| Feature | API 14 behaviour |
|---|---|
| `TvInputManager` HDMI/AV enumeration (API 21) | Empty. Use `input_sources` + `input_action`, `source_panel_*`, or the TV's own input picker. The Inputs pill stays hidden if none is configured. |
| `DreamService` screensaver (API 17) | The system screensaver entry is disabled. Home's own idle clock (`home_idle_clock_minutes`, `ClockActivity`) still works. |
| `NotificationListenerService` revival (API 18) | `GuardRevival` is disabled. |
| Accessibility key-event filtering (API 18) | The services bind, but remote keys cannot be filtered/re-injected; `key_filter`, `input_key_keycodes` and `app_keys` key filtering are inactive. |
| `WRITE_SECURE_SETTINGS` restore (`ServiceRepair`, guard) | Runs only if the installer could grant the permission; otherwise the helpers stay as the TV set them. `pm grant` does not exist before API 23. |
| Direct-boot storage / `UserManager` (API 24) | Not needed; preferences are read at once. |
| `Settings.Global.BOOT_COUNT` (API 17) | Open-on-start / boot probes stay off. |
| Locale override (`createConfigurationContext`, API 17) | On API 14-16 the app's resources are updated in place and the default locale is set; some system-provided texts may stay in the TV language. Hebrew/Arabic layouts (`layout-iw-v17`) are only used from API 17; older sets use the LTR layout with translated strings. |
| Material theme (API 21) | Android 5+ gets `Theme.Material`; API 14-20 use `Theme.Holo.NoActionBar.Fullscreen`. All launcher surfaces are custom-drawn, so the visible difference is small. |
| Vector icons (API 21) | The launcher ships PNG fallbacks at mdpi-hdpi-xhdpi-xxhdpi-xxxhdpi; API 21+ still uses the vectors. |
| Bundled Outfit font (res/font, API 26) | API 26+ uses Outfit; older sets use `sans-serif` / `sans-serif-light` / `sans-serif-medium` (where each exists). |
| Elevation, translationZ, clip-to-outline, letter spacing | No-ops below their API levels; shadows/zooms are simpler. `advanced_effects` is still honoured where the effect exists. |
| `TextClock` (API 17) | Replaced by `ClockText`, a TextView updated from `ACTION_TIME_TICK`/time changes. |
| `AtomicFile` (API 17) | Replaced by a small same-package `AtomicFile`; read/write and `.bak` recovery behave the same. |
| `java.nio.file.Files` (API 26) | `WallpaperCaptions` reads with `BufferedReader`. |
| `String.join`, `Math.floorMod`, `Objects.equals` (API 24/19) | Replaced with local helpers. |

## Performance notes

- 4.0 TVs are Dalvik, often 512 MB-1 GB RAM. Set `reduced_motion=true` and, if needed,
  `advanced_effects=false` in `config.xml` (or a theme) to drop the heavier fades and zooms.
- The APK is about 850 KB with the desugared library and multidex; that is intentional and still small
  for the storage these sets have.
- `WallpaperPlayer` still decodes wallpapers off the main thread; the hardware-bitmap path is skipped
  below API 26.

## Restore

Disabling packages is reversible. On a 4.0 set, re-enable the stock launcher with `pm enable <package>`
and press Home; the debloat scripts' restore path still applies. Nothing here uninstalls apps or needs
root.
