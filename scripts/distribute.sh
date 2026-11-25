#!/usr/bin/env bash
# Builds the app and uploads it to Firebase App Distribution.
#
#   scripts/distribute.sh                          # the release build (production, upload key)
#   scripts/distribute.sh debug                    # the debug build (staging, can switch servers)
#   scripts/distribute.sh both                     # both
#   scripts/distribute.sh both "Fixed checkout"    # release notes: this text, instead of the commits
#
# Without notes, each build's notes are the commits since that build's last upload.
# Needs FIREBASE_APP_ID (release) and/or FIREBASE_DEBUG_APP_ID (debug), FIREBASE_TESTER_GROUPS, either
# FIREBASE_SERVICE_ACCOUNT_FILE or a `firebase login`, and for release the upload key settings, in local.properties
# or the environment. See README, "Sending builds to testers".
set -euo pipefail

cd "$(dirname "$0")/.."

builds=(release)
case "${1:-}" in
  release) shift ;;
  debug) builds=(debug); shift ;;
  both) builds=(release debug); shift ;;
esac

# Android Studio's JDK when no other Java is set up.
if [[ -z "${JAVA_HOME:-}" ]] && ! /usr/libexec/java_home >/dev/null 2>&1; then
  studio_jdk="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  [[ -d "$studio_jdk" ]] && export JAVA_HOME="$studio_jdk"
fi

setting() { # local.properties first, then the environment, like the Gradle build
  local value
  value=$(grep -E "^$1=" local.properties 2>/dev/null | tail -1 | cut -d= -f2- || true)
  echo "${value:-${!1:-}}"
}

for build in "${builds[@]}"; do
  id_setting=FIREBASE_APP_ID
  [[ "$build" == debug ]] && id_setting=FIREBASE_DEBUG_APP_ID
  if [[ -z "$(setting "$id_setting")" ]]; then
    echo "$id_setting is not set. Add it to local.properties (see README, \"Sending builds to testers\")." >&2
    exit 1
  fi
done

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Note: uncommitted changes are included in this build." >&2
fi

# Every build gets a higher version code, so testers' phones install it over the last one.
version_code=$(git rev-list --count HEAD)
notes_file=app/build/firebase/release-notes.txt
mkdir -p "$(dirname "$notes_file")"

# One build at a time: both share the notes file, and each has its own notes.
for build in "${builds[@]}"; do
  task_build=$(tr '[:lower:]' '[:upper:]' <<< "${build:0:1}")${build:1}
  tag="firebase-latest-$build"

  # The text given, or the commits since this build's last upload (marked by its tag).
  if [[ $# -gt 0 ]]; then
    printf '%s\n' "$*" > "$notes_file"
  elif git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
    git log --no-merges --format='- %s' "$tag..HEAD" > "$notes_file"
  else
    git log --no-merges --format='- %s' -10 > "$notes_file"
  fi
  [[ -s "$notes_file" ]] || echo "- No changes since the last upload" > "$notes_file"

  echo "Building and uploading the $build build, version code $version_code ($(git rev-parse --short HEAD)). Notes:"
  cat "$notes_file"

  ./gradlew ":app:assemble$task_build" ":app:appDistributionUpload$task_build" -PversionCode="$version_code"

  # Mark this commit, so this build's next notes start here. Local only: never pushed.
  git tag -f "$tag" HEAD >/dev/null
  echo "Uploaded the $build build."
done

echo "Done. The testers in the chosen groups get an e-mail; the builds are also in the Firebase console."
