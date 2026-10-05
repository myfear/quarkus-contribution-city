# Release the action

Releases follow [Semantic Versioning](https://semver.org/) with a `v` prefix. The public contract is the action's inputs, defaults, outputs, generated SVG path, and documented runtime requirements.

| Change | Example |
| --- | --- |
| Compatible fix | `v1.0.0` → `v1.0.1` |
| Compatible feature or optional input | `v1.0.1` → `v1.1.0` |
| Breaking input, output, default, or runtime change | `v1.1.0` → `v2.0.0` |
| Preview release | `v1.1.0-alpha.1`, `v1.1.0-beta.1`, `v1.1.0-rc.1` |

Use all three version numbers, including for previews. Numeric identifiers must use their normal form: `1`, `2`, and `3`. The release workflow accepts SemVer tags with an optional prerelease suffix; build metadata (`+...`) is outside this project's tag convention. Major version zero is available for early development, when the public contract can change between releases.

## Verify a release

1. Merge the intended changes to `main` and review the CI result. The workflow releases the exact `main` commit selected when the run starts.
2. Open **Actions → Release action → Run workflow**.
3. Select `main`, enter a version such as `v1.0.0`, and keep **dry_run** selected.
4. Read the run summary. It shows the commit, prerelease status, whether the release becomes **Latest**, and whether the major tag advances. The full Java test suite runs, including release policy checks and publishing tests against a temporary local Git repository.

The dry run creates a verification report artifact. Tags and GitHub releases are created only by the publishing job.

You can also start a dry run with the GitHub CLI:

```bash
gh workflow run release.yml --repo myfear/quarkus-contribution-city \
  --ref main -f version=v1.0.0 -f dry_run=true
```

## Publish a verified version

Run the workflow again with the same version and clear **dry_run**. This run verifies its selected commit again. When verification succeeds, the publishing job:

1. Creates an annotated full version tag on the tested commit, with Markus Eisele as tagger.
2. Creates a GitHub release with generated release notes.
3. Marks tags with a suffix as prereleases. Stable releases become **Latest** when their version is at least as high as the other published stable versions.
4. Advances the major tag, such as `v1`, when the release is the newest stable version in that major. An older release in another major keeps the current global **Latest** release in place.

Only the publishing job receives `contents: write`. Release runs are serialized to keep version tags and aliases consistent. A major tag update uses a Git lease so an unexpected concurrent change causes a failure.

Full version tags stay fixed. If a run stops after creating a tag or release, rerun that original workflow run so it uses the same commit. The publisher checks the existing tag and release state before continuing. A tag pointing at another commit requires a new version. If an existing draft or release has different prerelease settings, review that release before retrying.

The Maven project version (`1.0-SNAPSHOT`) names the internal build artifact. Git tags version the action consumed by `uses:`. Keep the POM and the JAR path in `action.yml` aligned if the internal artifact name changes.

## List the action in GitHub Marketplace

The repository is public and contains the action metadata at its root. `action.yml` includes the action name, description, author, and icon. GitHub checks name availability in the Marketplace release form.

After publishing the first release, open its release editor and select **Publish this Action to the GitHub Marketplace**. The repository owner must accept the GitHub Marketplace Developer Agreement if it is still pending, choose the listing categories, and resolve any metadata messages shown by GitHub. The project is licensed under the [Apache License, Version 2.0](LICENSE).

Follow [GitHub's Marketplace publishing instructions](https://docs.github.com/en/actions/how-tos/create-and-publish-actions/publish-in-github-marketplace) for the current UI. The release workflow publishes GitHub releases; Marketplace enrollment is completed in that form.

The README and consumer examples use `@v1`. Update them when a new stable major version is published. The daily generator in this repository continues using the checked-out local action so it exercises `main`.

GitHub documents this combination of fixed version tags and moving major tags in [Managing custom actions](https://docs.github.com/en/actions/how-tos/create-and-publish-actions/manage-custom-actions).
