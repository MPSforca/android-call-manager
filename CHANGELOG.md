# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-11-08

### Added

- Privacy policy hosted on GitHub Pages (<https://mpsforca.github.io/android-call-manager/privacy/>),
  linked from Settings and the first-run permissions page.
- Portuguese (Brazil) and Spanish translations for the Rules screen, and a per-app language picker
  (Android 13+). Other screens follow as they move to string resources.
- Public-repository documentation: README, CONTRIBUTING, architecture docs and ADRs, privacy and
  security policies, code of conduct, Apache-2.0 license.
- CI (build, tests, static analysis, PR-title lint), release workflow, and dependency update
  automation.

### Changed

- Internals reorganized into `core/`, `feature/` and `screening/` packages with Hilt dependency
  injection and one ViewModel per feature. No behavior change.

## [1.0] - Initial MVP

- Call screening via `CallScreeningService` with block list, allow list, name patterns, known
  contacts, recently answered and recently called rules.
- Silence-or-reject setting, in-app blocked-call list merged with the system call log, batch blocking
  from call history.
