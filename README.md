# Quarkus Contribution City

A Java 21 GitHub Action that turns a GitHub contribution calendar into an SVG city. Each week becomes a building, and daily activity supplies its window pattern.

Companion project for [The Main Thread](https://www.the-main-thread.com), where the walkthrough and articles are published.

[![CI build](https://github.com/myfear/quarkus-contribution-city/actions/workflows/ci.yml/badge.svg)](https://github.com/myfear/quarkus-contribution-city/actions/workflows/ci.yml)

## Live example: @myfear

This city uses [@myfear's real GitHub contribution data](https://github.com/myfear) and is displayed in the [myfear/myfear profile README](https://github.com/myfear/myfear#readme).

[![Live GitHub contribution city for myfear](https://raw.githubusercontent.com/myfear/quarkus-contribution-city/output/contribution-city.svg)](https://github.com/myfear)

The [Contribution city workflow](https://github.com/myfear/quarkus-contribution-city/actions/workflows/contribution-city.yml) runs in this repository, refreshes the image daily at 03:17 UTC, and saves it on the `output` branch. The profile README embeds that image through its URL.

The visual idea is inspired by [Pink Pixel's skyline](https://github.com/pinkpixel-dev/skyline); this implementation is written independently in Java.

## Build and test

Install JDK 21, [JBang](https://www.jbang.dev/download/), Git, and Bash. Run from the repository root:

```bash
./mvnw verify
```

The tests run with Java, including the packaged application through Java and JBang, image publishing, and release behavior against temporary Git repositories. Saved calendar data and local HTTP servers supply the test responses. Preview images are written to `target/previews/`.

The project uses Quarkus 3.33.1, Quarkus GitHub Action 2.10.0, and platform-managed JUnit 6.0.3.

## Generate a city

Add this step to a GitHub workflow:

```yaml
- name: Generate contribution city
  id: city
  uses: myfear/quarkus-contribution-city@v1
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
    username: ${{ github.repository_owner }}
    weeks: '53'
    height: '14'
    theme: github-dark
```

Pin a reviewed commit in place of `v1` for an exact revision. The action builds that source revision and runs its packaged JAR with JBang.

- `username`: GitHub user to render. Use a user account rather than an organization.
- `github-token`: Token used to read the calendar. Its permissions and the user's visibility settings determine the available contributions. See [GitHub's contribution schema](https://docs.github.com/en/graphql/reference/users#contributionscollection) for private and internal contribution access.
- `weeks`: Recent weeks to render, from 4 to 53. Default: `53`.
- `height`: Maximum window rows, from 3 to 30. Default: `14`.
- `theme`: `github-dark` or `mono`. Default: `github-dark`.

The action writes `contribution-city.svg` in the caller's workspace and exposes `svg-path` and `total-contributions` outputs. The total covers the full returned calendar. The same calendar and options produce identical SVG bytes.

## Scheduled publishing

This repository's [Contribution city workflow](.github/workflows/contribution-city.yml) runs daily at 03:17 UTC and can be started manually from the Actions tab. It generates the repository owner's city and publishes only changed images to the `output` branch. A manual run can select another username. Each successful run also provides a downloadable SVG artifact.

The workflow uses the repository's built-in `GITHUB_TOKEN`. To include contributions that need additional access, supply a suitable token in the optional `CONTRIBUTION_TOKEN` repository secret. The job receives `contents: write` for publishing its output branch.

[View the generated city](https://raw.githubusercontent.com/myfear/quarkus-contribution-city/output/contribution-city.svg).

To run this from a profile repository, copy [examples/publish.yml](examples/publish.yml) into its `.github/workflows/` directory. [examples/generate.yml](examples/generate.yml) provides generation and artifact upload without committing an output branch.

Embed the image published by this repository in a README:

```markdown
![My GitHub contribution city](https://raw.githubusercontent.com/myfear/quarkus-contribution-city/output/contribution-city.svg)
```

SVG generation uses local font fallbacks, so character shapes can vary between viewers. The windows repeat each week's returned day pattern; use calendar counts for exact comparisons.

## Releases

The [Release action workflow](https://github.com/myfear/quarkus-contribution-city/actions/workflows/release.yml) verifies a selected commit on `main` before publishing. A dry run is selected by default. See the [release guide](RELEASING.md) for version selection, publishing, and GitHub Marketplace setup.

Release tags use `vMAJOR.MINOR.PATCH`, for example `v1.0.0`. Preview versions add a suffix such as `v1.1.0-beta.1`. Each full version tag stays on its original commit. Stable releases also maintain a major tag such as `v1`, which tracks the newest stable release in that major version. Prereleases keep their full version tag.

The examples use `v1` for compatible updates. [Version v1.0.0](https://github.com/myfear/quarkus-contribution-city/releases/tag/v1.0.0) is the first stable release. Pin `2fe22f7ed1d82f9dc1b4380c2b45769f1463a1d8` for that exact revision.

## License

Copyright 2026 Markus Eisele.

Licensed under the [Apache License, Version 2.0](LICENSE).
