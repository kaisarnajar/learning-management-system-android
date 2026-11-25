#!/usr/bin/env bash
# Builds the release APK (production, signed with the upload key) and uploads it to Firebase App Distribution.
#
#   scripts/distribute.sh                     # release notes: the commits since the last upload
#   scripts/distribute.sh "Fixed checkout"    # release notes: this text
#
# Needs FIREBASE_APP_ID, the upload key settings, and either FIREBASE_SERVICE_ACCOUNT_FILE or a `firebase login`,
# in local.properties or the environment. See README, "Sending builds to testers".
set -euo pipefail

cd "$(dirname "$0")/.."

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

if [[ -z "$(setting FIREBASE_APP_ID)" ]]; then
  echo "FIREBASE_APP_ID is not set. Add it to local.properties (see README, \"Sending builds to testers\")." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Note: uncommitted changes are included in this build." >&2
fi

# Every build gets a higher version code, so testers' phones install it over the last one.
version_code=$(git rev-list --count HEAD)

# The commits since the last upload (marked by the firebase-latest tag), or the text given.
notes_file=app/build/firebase/release-notes.txt
mkdir -p "$(dirname "$notes_file")"
if [[ $# -gt 0 ]]; then
  printf '%s\n' "$*" > "$notes_file"
elif git rev-parse -q --verify refs/tags/firebase-latest >/dev/null; then
  git log --no-merges --format='- %s' firebase-latest..HEAD > "$notes_file"
else
  git log --no-merges --format='- %s' -10 > "$notes_file"
fi
[[ -s "$notes_file" ]] || echo "- No changes since the last upload" > "$notes_file"

echo "Building and uploading version code $version_code ($(git rev-parse --short HEAD)). Release notes:"
cat "$notes_file"

./gradlew :app:assembleRelease :app:appDistributionUploadRelease -PversionCode="$version_code"

# Mark this commit, so the next upload's notes start here. Local only: never pushed.
git tag -f firebase-latest HEAD >/dev/null
echo "Uploaded. Testers in the chosen groups get an e-mail; the build is also in the Firebase console."
