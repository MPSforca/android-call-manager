# Contributing

Thanks for helping out. This guide covers how to set up, what a good PR looks like, and what
"done" means here. By participating you agree to the [Code of Conduct](CODE_OF_CONDUCT.md).

## Setup

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md). The one gotcha: **Gradle needs a JDK between 17 and
21**. A newer default JDK will make `./gradlew` fail immediately.

Before proposing a change to blocking behavior, read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
Several decisions there (no foreground service, no VoIP screening, no default-dialer role) are
deliberate and not up for reversal without a new ADR.

## Workflow

1. Open or find an issue for anything non-trivial, so we can agree on the approach first.
2. Fork (or branch, if you have access) from the latest `main`. Use a
   [git worktree](https://git-scm.com/docs/git-worktree) per task if you juggle several:
   ```bash
   git fetch origin
   git worktree add .worktrees/<task-name> -b <type>/<task-name> origin/main
   ```
3. Branch names: `feat/`, `fix/`, `chore/`, `docs/`, `refactor/`, `test/` + a short slug.
4. Commit with [Conventional Commits](https://www.conventionalcommits.org/) (`feat: ...`,
   `fix(rules): ...`). PRs are squash-merged and **the PR title becomes the commit message**; CI
   rejects titles that aren't conventional.
5. Open the PR as a **draft**, fill in the template, and keep it green. Mark it ready when CI passes
   and you want review.

## Checks to run before pushing

```bash
export JAVA_HOME=/path/to/jdk-17-to-21

./gradlew ktlintCheck detekt lintDebug
./gradlew assembleDebug testDebugUnitTest
```

`./gradlew ktlintFormat` fixes most style issues automatically. These same checks run in CI
(`build`, `test`, `static-analysis`, `pr-title`) and all must pass.

## Code style

- Kotlin official style, enforced by ktlint (`.editorconfig`) and detekt (`config/detekt/`).
- Match the surrounding code: naming, comment density, idiom.
- Keep rule and merge logic free of Android types where possible; it stays cheap to unit test.
- Don't add a lint/detekt suppression or baseline entry to get green without explaining why in the
  PR.

## Definition of done

- Lint, build and unit tests pass locally and in CI.
- **New and changed lines have at least 90% test coverage.** This applies to the lines you touched,
  not the whole repo. Code that can't run on the JVM (raw `CallLog` queries, for instance; see
  [docs/TESTING.md](docs/TESTING.md)) is exempt, but say so in the PR and describe the manual test.
- **User-visible text is localized.** No hardcoded UI strings: every string lives in the feature's
  `res/values/strings_<feature>.xml` (English) with matching `values-pt-rBR` and `values-es`
  entries, and counts use `<plurals>`.
- Behavior or UI changes were tried on a device or emulator, and the PR says how.
- Docs are updated if behavior, permissions or architecture changed, and `CHANGELOG.md` has an
  entry under *Unreleased* for user-visible changes.
- The PR description explains *why*, not just what.

## Reporting bugs and requesting features

Use the issue templates. For security problems, don't open an issue; see
[SECURITY.md](SECURITY.md).
