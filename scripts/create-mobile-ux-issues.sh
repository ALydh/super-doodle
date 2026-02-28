#!/bin/bash
# Creates GitHub issues for mobile UX findings.
# Prerequisites: gh CLI authenticated (run `gh auth login` first)
#
# Usage: ./scripts/create-mobile-ux-issues.sh

set -euo pipefail

REPO="ALydh/super-doodle"

create_issue() {
  local title="$1"
  local file="$2"

  # Check if issue already exists
  existing=$(gh issue list --repo "$REPO" --search "\"$title\" in:title" --json number --jq '.[0].number // empty' 2>/dev/null || true)
  if [ -n "$existing" ]; then
    echo "  SKIP: Issue #$existing already exists — $title"
    return
  fi

  body=$(tail -n +2 "$file")
  gh issue create --repo "$REPO" --title "$title" --body "$body"
  echo "  CREATED: $title"
}

echo "Creating mobile UX issues for $REPO..."
echo ""

for file in .github/mobile-ux-issues/*.md; do
  [ -f "$file" ] || continue
  title=$(head -1 "$file" | sed 's/^# //')
  echo "Processing: $title"
  create_issue "$title" "$file"
  echo ""
done

echo "Done! View issues: gh issue list --repo $REPO"
