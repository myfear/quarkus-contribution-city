#!/usr/bin/env bash
set -euo pipefail

# Run in the profile checkout after generation. Publish in a separate temporary checkout.
source_image="$PWD/contribution-city.svg"
workspace="${GITHUB_WORKSPACE:-$PWD}"
repository_url="$(git -C "$workspace" remote get-url origin)"
publish_dir="$(mktemp -d)"
trap 'rm -rf "$publish_dir"' EXIT
git init --quiet --initial-branch=output "$publish_dir"
git -C "$publish_dir" remote add origin "$repository_url"

# actions/checkout installs this header. Copy it without printing it or putting it in a remote URL.
header="$(git -C "$workspace" config --get http.https://github.com/.extraheader || true)"
if [ -n "$header" ]; then
  git -C "$publish_dir" config http.https://github.com/.extraheader "$header"
fi

# Authentication/network failures abort; an empty successful result means the branch is new.
refs="$(git -C "$publish_dir" ls-remote --heads origin refs/heads/output)"
if [ -n "$refs" ]; then
  git -C "$publish_dir" fetch --quiet --depth=1 origin output
  git -C "$publish_dir" checkout --quiet -B output FETCH_HEAD
fi
cp "$source_image" "$publish_dir/contribution-city.svg"
git -C "$publish_dir" add -- contribution-city.svg
if git -C "$publish_dir" diff --cached --quiet; then
  echo 'Contribution city is unchanged.'
  exit 0
fi
git -C "$publish_dir" config user.name 'github-actions[bot]'
git -C "$publish_dir" config user.email '41898282+github-actions[bot]@users.noreply.github.com'
git -C "$publish_dir" commit --quiet -m 'Update contribution city'
git -C "$publish_dir" push --quiet origin HEAD:output
