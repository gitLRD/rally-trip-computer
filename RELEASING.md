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

AutoName: Rally Trip Computer

RepoType: git
Repo: https://github.com/gitLRD/rally-trip-computer.git

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.0.0
CurrentVersionCode: 1
```

`UpdateCheckMode: Tags` finds new releases from the repository's tags, and `AutoUpdateMode:
Version` then adds a build block for each one, so future releases are picked up without
touching this file again.

`Version` takes **no pattern** here. A pattern like `Version v%v` is only for
`UpdateCheckMode: HTTP`, where the tag name has to be reconstructed from a version string;
with `Tags` the tag that was found is used directly, and adding a pattern is a lint error.

`AutoName` has to be written out even though nothing here sets it by hand. `fdroid
checkupdates` derives it from `android:label` in the manifest, and fdroiddata's CI runs
`checkupdates` and then `git diff --exit-code` — so the file must already contain whatever
the tool would generate, or the `checkupdates` job fails. If the app is ever renamed,
update this line to match the new label. Its position matters too: `fdroid rewritemeta`
enforces a canonical field order in which `AutoName` sits in its own block between
`IssueTracker` and `RepoType`.

Before opening the merge request it is worth running F-Droid's own lint, which catches most
of what a reviewer would otherwise bounce it for:

```sh
fdroid lint io.github.gitlrd.rallytripcomputer
fdroid build io.github.gitlrd.rallytripcomputer:1
```

### Make the source repository public first

`fdroid build` clones `Repo` anonymously. If <https://github.com/gitLRD/rally-trip-computer>
is private the clone fails with `Invalid username or token`, and `fdroid build`,
`checkupdates` and `check source code` all fail together within seconds. Note that a local
`git ls-remote` is not a useful check here: it succeeds against a private repo because it
picks up the credential helper. Test the way CI does, without credentials:

```sh
GIT_CONFIG_GLOBAL=/dev/null GIT_CONFIG_SYSTEM=/dev/null \
  git ls-remote https://github.com/gitLRD/rally-trip-computer.git
```

### If the build is rejected

`compileSdk = 36` is not a problem: the buildserver installs the platform on demand, and
version 1.0.0 built there successfully on JDK 21 with AGP 8.10.1. It is set ahead of
`targetSdk = 34` on purpose — androidx releases increasingly require compiling against 36 —
so if a future buildserver ever cannot supply it, ask on the merge request rather than
lowering it, since lowering it breaks the dependency updates it exists to allow.

Note that the `YAML 1.2` CI job does not check this file. It is scoped to `.gitlab-ci.yml`
and `config/`; app recipes are validated by `schema validation`, `fdroid lint` and
`fdroid rewritemeta` instead.
