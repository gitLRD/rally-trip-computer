# Releasing

Pushing a `v*` tag runs [`.github/workflows/release.yml`](.github/workflows/release.yml),
which runs the test suite, builds a signed APK and publishes it as a GitHub release.

This is a one-off setup followed by a two-command release.

## One-off: create the signing key

Android identifies an app by its signature, not its name. Every future update has to be
signed with **this same key**, or phones will refuse to install it over the existing app.
Losing the keystore means nobody can upgrade — they have to uninstall first, losing their
trips and settings. Back it up somewhere you will still have in five years.

```sh
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias gps-trip-computer \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -storetype PKCS12
```

Keep `release.keystore` out of the repository. It is not in `.gitignore` by name because it
should never be anywhere near the working tree in the first place.

## One-off: add the repository secrets

```sh
base64 -i release.keystore | gh secret set RELEASE_KEYSTORE_BASE64
gh secret set RELEASE_KEYSTORE_PASSWORD    # the -storepass you chose
gh secret set RELEASE_KEY_ALIAS            # gps-trip-computer
gh secret set RELEASE_KEY_PASSWORD         # the -keypass you chose
```

Without these the release workflow stops with an explanatory error rather than publishing
an unsigned APK that nobody can install.

## Each release

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`. The workflow refuses to
   run if the tag and `versionName` disagree, so they cannot drift.
2. Write `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`. Those notes become
   both the GitHub release body and the F-Droid changelog, so they never diverge.
3. Tag and push:

```sh
git tag v1.1.0
git push origin v1.1.0
```

## Local release builds

`./gradlew assembleRelease` on a machine with none of the `RELEASE_*` environment variables
set produces an **unsigned** APK, at `app/build/outputs/apk/release/app-release-unsigned.apk`.
That is deliberate — it is what F-Droid wants, since it builds from source and signs with
its own key — but it also means a locally built release APK cannot be sideloaded. Use
`assembleDebug` for that.

The unsigned build lands under a different filename from the signed one
(`app-release.apk`), which is a useful safety net: the release workflow only ever looks for
the signed name, so it cannot publish an unsigned APK even if the signing step were skipped.

## A note on F-Droid

F-Droid builds from source and signs with F-Droid's key, so an APK installed from F-Droid
and an APK downloaded from a GitHub release have different signatures. A phone will not
upgrade one to the other; switching sources means uninstalling first. Pick one to recommend
in the README rather than leaving people to find out at the roadside.
