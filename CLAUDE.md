# Call Manager

Android app (Kotlin + Jetpack Compose + Hilt + Room) that acts as the device's
`CallScreeningService` to add rule-based call blocking beyond what stock Android offers, while
keeping every blocked call visible in-app.

This file orients contributors and coding agents. Details live in the docs it links to; keep this
file short and don't duplicate them here.

## Where to read

- [README.md](README.md): what the app does, rule order, permissions and why.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): components, rule evaluation, known limitations.
- [docs/adr/](docs/adr/README.md): the reasoning behind the big decisions.
- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md): environment setup, build, troubleshooting.
- [docs/TESTING.md](docs/TESTING.md): unit tests, adb fixtures, manual recipes.
- [CONTRIBUTING.md](CONTRIBUTING.md): workflow and definition of done.

## Layout

Single `:app` module, package-by-feature, under `app/src/main/java/com/sforca/callmanager/`:
`core/` (model, database, datastore, telephony, data, designsystem, di), `feature/` (recent, lists,
rules, debug, onboarding), `screening/` (the screening service and phone-state receiver).

Rule logic lives in `core/data/CallRepository.evaluate()`. `ScreeningPolicy` and `BlockedCallMerger`
are pure functions with no Android types; keep new rule and merge logic Android-free so it stays
cheap to unit test. System integrations (contacts, outgoing calls) sit behind interfaces so tests
can use fakes. Dependencies are injected with Hilt; don't reintroduce singletons.

## Decisions not to relitigate without reading the ADR

- **No foreground or persistent service** ([ADR 0001](docs/adr/0001-no-foreground-service.md)).
- **Only real SIM/carrier calls are screened, never VoIP.** Do not add
  `CALL_SCREENING_INCLUDE_SELF_MANAGED_CALLS` to the manifest without an explicit request
  ([ADR 0002](docs/adr/0002-no-voip-screening.md)).
- **Blocked calls merge our Room log with the system call log; no default-dialer role**
  ([ADR 0003](docs/adr/0003-blocked-calls-merge-sources.md)).
- **Silence by default; full-screen vs notification rendering is the dialer's decision, not ours**
  ([ADR 0004](docs/adr/0004-silence-vs-reject.md)).

## Localization

The app ships in English, Portuguese (`pt-BR` only) and Spanish. **Any task that adds or changes
UI text must also add the translations:**

- No hardcoded strings in Composables, notifications or `contentDescription`s. Use
  `stringResource` / `pluralStringResource`, with the string in `res/values/strings_<feature>.xml`
  (one file per feature, to avoid merge conflicts).
- Add the same keys to `res/values-pt-rBR/` and `res/values-es/` in the same PR. Use `<plurals>`
  for counts.
- Check that longer translations don't truncate or break layouts.

## Build and test

Gradle needs a **JDK 17–21** (newer JDKs fail); set `JAVA_HOME` accordingly. See
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

```bash
./gradlew ktlintCheck detekt lintDebug
./gradlew assembleDebug testDebugUnitTest
```

- Robolectric is pinned to `sdk = [34]` via `@Config` on `CallRepositoryTest`, because Robolectric
  4.13 doesn't support API 35 (the project's `compileSdk`/`targetSdk`). Bump only together with a
  Robolectric upgrade.
- Raw `CallLog.Calls.CONTENT_URI` queries in `CallLogUtil` are not Robolectric-testable; they are
  covered by the manual adb recipes in `docs/TESTING.md`.

## Workflow

**Use a git worktree for each new task.** Don't work directly on `main` or on whatever branch is
checked out. Fetch first so the branch point is current, and branch from `origin/main`, not a
possibly stale local `main`:

```bash
git fetch origin
git worktree add .worktrees/<task-name> -b <type>/<task-name> origin/main
```

**Before a task is done:**

1. Lint and build pass locally in the worktree: `./gradlew ktlintCheck detekt lintDebug` and
   `./gradlew assembleDebug testDebugUnitTest`.
2. At least 90% coverage on new and changed lines of the files the task touches (not a repo-wide
   bar).
3. Open the PR as a **draft** (`gh pr create --draft`) with a Conventional Commits title, then watch
   CI (`gh pr checks --watch`) and fix failures. Mark it ready for review only when a maintainer
   asks, or when the contributor decides it is.
