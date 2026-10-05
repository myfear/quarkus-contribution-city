#!/usr/bin/env bash
set -euo pipefail

mode="${1:?Expected plan or publish}"
case "$mode" in plan|publish) ;; *) exit 2 ;; esac
: "${TAG:?A version tag is required}"
: "${GH_REPO:?A repository is required}"
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT

# Java owns version validation and ordering; GitHub supplies the published versions.
gh api --paginate "repos/$GH_REPO/releases?per_page=100" \
  --jq '.[] | select(.draft == false and .prerelease == false) | .tag_name' > "$scratch/stable-tags"
java .github/release/ReleaseVersion.java "$TAG" "$scratch/stable-tags" "$scratch/plan"
cat "$scratch/plan"
if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  cat "$scratch/plan" >> "$GITHUB_OUTPUT"
fi
if [[ "$mode" == plan ]]; then
  exit 0
fi

: "${GITHUB_SHA:?An exact commit is required}"
[[ "${GITHUB_REF:-}" == refs/heads/main ]] || { echo 'Release from main.' >&2; exit 1; }
# Every value in this file comes from the strict Java parser above.
source "$scratch/plan"
git fetch origin main --tags
git merge-base --is-ancestor "$GITHUB_SHA" origin/main
[[ "$(git rev-parse HEAD)" == "$GITHUB_SHA" ]] || { echo 'Checkout does not match the tested commit.' >&2; exit 1; }
git diff --exit-code

if git show-ref --verify --quiet "refs/tags/$tag"; then
  [[ "$(git rev-parse "$tag^{commit}")" == "$GITHUB_SHA" ]] || {
    echo "Release tag $tag already identifies another commit. Choose a new version." >&2
    exit 1
  }
else
  git -c user.name='Markus Eisele' -c user.email='markus@eisele.net' tag -a "$tag" "$GITHUB_SHA" -m "Release $tag"
  git push origin "refs/tags/$tag"
fi

if existing=$(gh release view "$tag" --repo "$GH_REPO" --json isDraft,isPrerelease --jq '[.isDraft,.isPrerelease] | @tsv'); then
  [[ "$existing" == $'false\t'"$prerelease" ]] || {
    echo 'An existing release has a different publication state. Review it before retrying.' >&2
    exit 1
  }
else
  flags=(--repo "$GH_REPO" --verify-tag --title "$tag" --generate-notes --latest="$latest")
  if [[ "$prerelease" == true ]]; then flags+=(--prerelease); fi
  gh release create "$tag" "${flags[@]}"
fi

if [[ "$advance_major" == true ]]; then
  previous=$(git rev-parse --verify "refs/tags/$major" 2>/dev/null || true)
  git update-ref "refs/tags/$major" "$GITHUB_SHA"
  git push --force-with-lease="refs/tags/$major:$previous" origin "refs/tags/$major"
fi
