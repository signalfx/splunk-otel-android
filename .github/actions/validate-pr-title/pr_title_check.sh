#!/bin/bash
set -e

# List of authors to skip
SKIP_AUTHORS=("app/renovate)

PR_AUTHOR="${GITHUB_ACTOR}"

for author in "${SKIP_AUTHORS[@]}"; do
  if [[ "$PR_AUTHOR" == "$author" ]]; then
    echo "PR authored by $PR_AUTHOR, skipping validation."
    exit 0
  fi
done

PR_TITLE="${GITHUB_EVENT_PULL_REQUEST_TITLE}"
echo "Validating PR title: \"$PR_TITLE\""

REGEX='^\[?(WIP|wip)?\]?\s*(DEMRUM-[0-9]+(,\s?DEMRUM-[0-9]+)*|NO-TICKET):\s.+$'

if [[ "$PR_TITLE" =~ $REGEX ]]; then
  echo "✅ PR title is valid."
else
  echo "❌ PR title is invalid."
  echo ""
  echo "It must match one of the following formats:"
  echo "- DEMRUM-1234: Description"
  echo "- DEMRUM-1234, DEMRUM-5678: Description"
  echo "- NO-TICKET: Description"
  exit 1
fi