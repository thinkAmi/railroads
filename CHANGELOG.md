<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# railroads Changelog

## [Unreleased]

- BREAKING CHANGE: Raised the minimum supported IDE version to 2025.3. IDE 2024.x, 2025.1, and 2025.2 are no longer supported.
- Improved compatibility with IDE 2026, fixing startup errors and route-loading/copy-action failures. ([#96](https://github.com/thinkAmi/railroads/pull/96), [#99](https://github.com/thinkAmi/railroads/pull/99), [#100](https://github.com/thinkAmi/railroads/pull/100))
- Rails Routes now shows a notification when the Ruby SDK or Rails configuration is missing or misconfigured, instead of failing silently. ([#97](https://github.com/thinkAmi/railroads/pull/97))
- Rails Routes now saves unsaved files before loading routes, making recent edits more reliably reflected in the route list. ([#98](https://github.com/thinkAmi/railroads/pull/98))
- Improved route resolution stability to avoid intermittent failures while loading routes. ([#98](https://github.com/thinkAmi/railroads/pull/98))

## [0.5.1] - 2026-02-08

- fix: change the icon to display for the route by @thinkAmi in https://github.com/thinkAmi/railroads/pull/70
- feat: Migrate to Kotlin Coroutines and support IDE 2025.3 by @thinkAmi in https://github.com/thinkAmi/railroads/pull/72
- fix: replace internal API usage by @thinkAmi in https://github.com/thinkAmi/railroads/pull/74


## [0.5.0] - 2026-02-07

- fix: change the icon to display for the route by @thinkAmi in https://github.com/thinkAmi/railroads/pull/70
- feat: Migrate to Kotlin Coroutines and support IDE 2025.3 by @thinkAmi in https://github.com/thinkAmi/railroads/pull/72

## [0.4.2] - 2025-11-26

- fix: protect PSI access with ReadAction and run route parsing off the EDT by @thinkAmi in https://github.com/thinkAmi/railroads/pull/66
- fix: missing ruby module dependency by @thinkAmi in https://github.com/thinkAmi/railroads/pull/68

## [0.4.0] - 2025-10-02

- Changelog update - `v0.3.0` by @github-actions[bot] in https://github.com/thinkAmi/railroads/pull/56
- add DeepWiki Badge by @thinkAmi in https://github.com/thinkAmi/railroads/pull/57
- Add double-click for route navigation by @COsborn2 in https://github.com/thinkAmi/railroads/pull/59
- Support IDE version 2025.2 by @thinkAmi in https://github.com/thinkAmi/railroads/pull/63
- @COsborn2 made their first contribution in https://github.com/thinkAmi/railroads/pull/59

## [0.3.0] - 2025-04-19

- Changelog update - `v0.2.1` by @github-actions in https://github.com/thinkAmi/railroads/pull/48
- [BREAKING CHANGE] Support IDE version 2024.2 and later by @thinkAmi in https://github.com/thinkAmi/railroads/pull/55

## [0.2.1] - 2024-09-03

- Resize plugin icon by @thinkAmi in https://github.com/thinkAmi/railroads/pull/47

## [0.1.0]

- Initial release

## [0.1.1]

### Fixed

- fix `pluginUntilBuild` to `Disable`

[Unreleased]: https://github.com/thinkAmi/railroads/compare/v0.5.1...HEAD
[0.5.1]: https://github.com/thinkAmi/railroads/compare/v0.5.0...v0.5.1
[0.5.0]: https://github.com/thinkAmi/railroads/compare/v0.4.2...v0.5.0
[0.4.2]: https://github.com/thinkAmi/railroads/compare/v0.4.0...v0.4.2
[0.4.0]: https://github.com/thinkAmi/railroads/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/thinkAmi/railroads/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/thinkAmi/railroads/compare/v0.1.0...v0.2.1
[0.1.1]: https://github.com/thinkAmi/railroads/commits/v0.1.1
[0.1.0]: https://github.com/thinkAmi/railroads/compare/v0.1.1...v0.1.0
