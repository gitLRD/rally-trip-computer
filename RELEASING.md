# Releasing

The app is distributed through **F-Droid**, which builds it from source on its own
buildserver and signs it with F-Droid's key. There is deliberately no keystore in this
project and no signing config in `app/build.gradle.kts`: `./gradlew assembleRelease`
produces `app-release-unsigned.apk`, which is exactly what F-Droid expects, and there is no
signing key to leak or to lose.

The consequence worth remembering: because F-Droid holds the signature, an APK built here
cannot be installed as an upgrade over an F-Droid install. If direct downloads are ever
wanted, existing F-Droid users would have to uninstall first, losing their trips. That is
the price of not carrying a keystore, and it was chosen deliberately.

For trying a build on your own phone, use `./gradlew assembleDebug` — debug builds are
signed with the standard debug key and sideload normally.

## Each release

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Write `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`. F-Droid reads these
   straight from the repository, so this is the only place release notes need to live.
3. Tag and push:

   ```sh
   git tag v1.1.0
   git push origin v1.1.0
   ```

F-Droid's `UpdateCheckMode: Tags` notices the new tag and builds it automatically. No
merge request is needed after the first one.

## First submission to F-Droid

This part is manual and needs a **GitLab** account — `fdroiddata` lives on GitLab, not
GitHub.

1. Fork <https://gitlab.com/fdroid/fdroiddata>.
2. Add `metadata/io.github.gitlrd.rallytripcomputer.yml` with the contents below.
3. Open a merge request against `fdroiddata`.

```yaml
Categories:
  - Navigation
  - Sports & Health
License: GPL-3.0-or-later
AuthorName: gitLRD
SourceCode: https://github.com/gitLRD/rally-trip-computer
IssueTracker: https://github.com/gitLRD/rally-trip-computer/issues

RepoType: git
Repo: https://github.com/gitLRD/rally-trip-computer.git

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
CurrentVersion: 1.0.0
CurrentVersionCode: 1
```

`AutoUpdateMode: Version v%v` tells F-Droid that tags are the version prefixed with `v`, so
future releases are picked up without touching this file again.

Before opening the merge request it is worth running F-Droid's own lint, which catches most
of what a reviewer would otherwise bounce it for:

```sh
fdroid lint io.github.gitlrd.rallytripcomputer
fdroid build io.github.gitlrd.rallytripcomputer:1
```

### If the build is rejected

The likeliest cause is `compileSdk = 36`, which needs a recent buildserver SDK. It is set
ahead of `targetSdk = 34` on purpose — androidx releases increasingly require compiling
against 36 — so if the buildserver cannot supply it, ask on the merge request rather than
lowering it, since lowering it breaks the dependency updates it exists to allow.
