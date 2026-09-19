#!/usr/bin/env sh
# Runs every module's tests against the reference solutions in solutions/.
# The starter project deliberately does not compile until the student has
# written the declarations the tests call, so this is the only way to check
# that the tests themselves are right.
set -eu

root=$(cd "$(dirname "$0")" && pwd)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

rsync -a --exclude build --exclude .idea --exclude .kotlin --exclude solutions "$root/" "$work/"
for module in "$root"/solutions/*/; do
  name=$(basename "$module")
  cp -R "$module/src/." "$work/$name/src/"
  if [ -d "$module/resources" ]; then
    mkdir -p "$work/$name/resources"
    cp -R "$module/resources/." "$work/$name/resources/"
  fi
done

cd "$work" && ./kotlin test "$@"
