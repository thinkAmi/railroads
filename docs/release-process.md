# Release Process Runbook

## Purpose
This runbook defines the only supported release procedure for this repository. It prevents duplicate releases, untagged draft URLs, and asset update issues under Immutable Releases.

## Scope
This document is for repository maintainers and release operators.

It covers:
- GitHub draft release creation and publication
- JetBrains Marketplace publication via GitHub Actions
- Release troubleshooting and recovery

It does not cover:
- Plugin feature development
- End-user installation instructions

## Required repository settings
- GitHub Releases `Immutable Releases` is enabled.
- The default branch is `main`.
- GitHub Actions workflows are enabled.
- `/Users/thinkami/project/railroads/.github/workflows/build.yml` runs on `main` pushes and creates draft releases.
- `/Users/thinkami/project/railroads/.github/workflows/release.yml` runs on `release` events (`prereleased`, `released`).

## Required secrets
The following repository secrets must be configured:
- `PUBLISH_TOKEN`
- `CERTIFICATE_CHAIN`
- `PRIVATE_KEY`
- `PRIVATE_KEY_PASSWORD`

## Release workflow overview
1. A merge to `main` triggers the `Build` workflow.
2. `releaseDraft` keeps only the current target version draft, resets the target draft/tag, and creates a new draft release with a zip asset already attached.
3. A maintainer reviews and publishes that draft from the Releases page.
4. Publishing the draft triggers the `Release` workflow.
5. The `Release` workflow runs `publishPlugin` and opens a changelog update PR.

## Standard release procedure
1. Update `pluginVersion` in `/Users/thinkami/project/railroads/gradle.properties`.
2. Update unreleased notes in `/Users/thinkami/project/railroads/CHANGELOG.md`.
3. Merge to `main`.
4. Confirm the `Build` workflow succeeded.
5. Open the target draft release and confirm:
- Tag is correct (`vX.Y.Z`)
- Exactly one plugin zip asset is attached
- Release notes look correct
6. Publish that existing draft release.
7. Confirm the `Release` workflow succeeded.
8. Confirm the version is published in JetBrains Marketplace.
9. Review and merge the changelog update PR created by the workflow.
10. If the changelog PR shows `Code scanning is waiting for results from CodeQL`, add a minimal no-op commit on that branch from the GitHub UI and wait for checks to refresh.

### Pre-publish checklist
- [ ] `pluginVersion` was bumped to the intended version.
- [ ] `Build` workflow succeeded on `main`.
- [ ] Exactly one draft exists for the target version.
- [ ] The draft has exactly one zip asset.
- [ ] The draft notes are correct.
- [ ] No manual duplicate release was created for the same tag.

### Post-publish checklist
- [ ] `Release` workflow succeeded.
- [ ] `publishPlugin` step succeeded.
- [ ] The new version is visible in JetBrains Marketplace.
- [ ] Changelog PR was created (when release body is non-empty).
- [ ] Changelog PR checks are green (or refreshed after a minimal no-op commit when CodeQL was waiting).
- [ ] Changelog PR is reviewed and merged.

## Version change scenarios
### `v0.5.2 -> v0.6.0`
When `pluginVersion` is updated from `0.5.2` to `0.6.0` and merged to `main`, non-target draft releases are removed automatically and `v0.6.0` is created as the only draft.

### `v0.5.2 -> v0.5.2` (same-version rerun)
When the same version is merged again, the workflow removes the existing target draft/tag and recreates the `v0.5.2` draft from the latest `main` commit.

### Published target already exists
If a non-draft release already exists for the target tag, `Build` intentionally fails with a guard message. Bump `pluginVersion` and rerun via a new merge.

## Do / Don't rules
Do:
- Publish the workflow-created draft release for the target tag.
- Treat the draft as the single source of truth for release publication.
- Verify assets and notes before publishing.

Don't:
- Do not create a brand-new non-draft release manually for a tag that already has a workflow-managed draft.
- Do not manually upload release assets after publish.
- Do not reuse an already published version tag.

## Troubleshooting
### `untagged-...` draft URL appears
Cause:
- A duplicate release path occurred, typically by mixing manual release creation with workflow-managed draft handling.

How to identify:
1. Check release metadata and creator.
2. Check `Build` workflow runs around the draft creation timestamp.
3. Confirm whether a manual published release with the same tag also exists.

Useful commands:
```bash
gh release list --repo thinkAmi/railroads --limit 50
gh api --paginate repos/thinkAmi/railroads/releases --jq '.[] | {id, draft, name, tag_name, html_url, created_at, published_at, author: .author.login}'
gh run list --repo thinkAmi/railroads --workflow build.yml --limit 20
```

### Build fails with "already exists and is not a draft"
Cause:
- The target version tag is already published.

Action:
1. Bump `pluginVersion` to a new version.
2. Merge to `main` again.

### Draft creation fails due to asset count check
Cause:
- The build did not produce exactly one zip in `build/distributions`.

Action:
1. Inspect the `Create Release Draft` step logs.
2. Fix build output conditions.
3. Merge and rerun.

### Changelog PR is blocked with `Code scanning is waiting for results from CodeQL`
Cause:
- The changelog PR was created by `release.yml` using `GITHUB_TOKEN`, and checks may not attach to the PR commit immediately.

Action:
1. Open the changelog PR in GitHub.
2. Edit `CHANGELOG.md` in the GitHub UI and make a minimal no-op text change.
3. Commit directly to the changelog PR branch (for example, `changelog-update-vX.Y.Z`).
4. Wait for checks to refresh, then merge the PR when all required checks are green.

## Recovery procedures
### Safe cleanup when duplicate draft and published release exist for the same version
1. Decide which published release is canonical.
2. Keep the canonical published release untouched.
3. Delete only the stale draft release by release ID.
4. Re-run release only through the workflow-managed draft path.

### Safe cleanup when the target is still draft-only and incorrect
1. Delete the incorrect draft release.
2. Delete the corresponding tag ref if it should be recreated.
3. Merge a correcting commit to `main` so `Build` recreates the draft.

## Post-release verification
After publishing:
1. Confirm the `Release` workflow run for the tag completed successfully.
2. Confirm `publishPlugin` succeeded.
3. Confirm the plugin version is visible and installable in JetBrains Marketplace.
4. Confirm release notes and attached asset in GitHub are correct.

## Appendix: useful gh commands
```bash
# List releases
gh release list --repo thinkAmi/railroads --limit 50

# View release details
gh release view vX.Y.Z --repo thinkAmi/railroads

# View all release metadata (with IDs)
gh api --paginate repos/thinkAmi/railroads/releases --jq '.[] | {id, draft, prerelease, name, tag_name, html_url, created_at, published_at}'

# List Build workflow runs
gh run list --repo thinkAmi/railroads --workflow build.yml --limit 20

# List Release workflow runs
gh run list --repo thinkAmi/railroads --workflow release.yml --limit 20

# Inspect a specific run
gh run view <run-id> --repo thinkAmi/railroads --json jobs
```
