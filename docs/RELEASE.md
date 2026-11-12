# Releasing Flipday

Nothing here has been run. Do it once, by hand, and keep the keystore and its passwords out of the repo (`*.jks` is already git-ignored).

## 1. Privacy policy URL (Play requires a public page)
`docs/privacy/index.html` is the policy in English and Arabic, generated from the in-app text. To publish it:
GitHub repo → Settings → Pages → Source: Deploy from a branch → `main` / `/docs`. The URL is then
`https://m-damer.github.io/flipday/privacy/`. If the in-app text in `strings.xml` (`privacy_body`) changes, update the page too.

## 2. Upload key
```
keytool -genkeypair -v -keystore flipday-upload.jks -alias flipday -keyalg RSA -keysize 2048 -validity 10000
```
Back the file up outside the repo. Enrol in Play App Signing when creating the app in Play Console, and upload this as the upload key.

## 3. Build a signed bundle
```
export FLIPDAY_KEYSTORE_PATH=/path/to/flipday-upload.jks
export FLIPDAY_KEYSTORE_PASSWORD=...   FLIPDAY_KEY_ALIAS=flipday   FLIPDAY_KEY_PASSWORD=...
gradle :app:bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`. Without the variables the release build is unsigned (CI builds it that way to check that R8 shrinking works).

## 4. Before the first upload
- Raise `versionCode` for every upload (`app/build.gradle.kts`).
- Install the release build on a real phone: R8 is on, and only CI has compiled it.
- Fill the listing from `docs/PLAY_LISTING.md`, then the Data safety form and the exact-alarm declaration if asked.

## Building the signed bundle on GitHub instead of your computer
`.github/workflows/release.yml` (Actions tab → "Release bundle" → Run workflow) builds and signs the bundle and attaches `flipday-release-aab` to the run. Add these four repository secrets first: `FLIPDAY_KEYSTORE_BASE64` (output of `base64 -w0 flipday-upload.jks`), `FLIPDAY_KEYSTORE_PASSWORD`, `FLIPDAY_KEY_ALIAS` (`flipday`), `FLIPDAY_KEY_PASSWORD`.
