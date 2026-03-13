#!/bin/bash
set -e

if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <flavor> <new_version (semantic versioning, e.g., 1.0.0)> <output_dir>"
    exit 1
fi

FLAVOR=$1
NEW_VERSION=$2
OUTPUT_DIR=$3

GRADLE_FILE="composeApp/build.gradle.kts"

# Read package name from gradle file
PACKAGE_NAME=$(grep -m 1 'applicationId =' "$GRADLE_FILE" | cut -d '"' -f 2)

# Extract current versionCode and increment it
CURRENT_VERSION_CODE=$(grep -E 'versionCode = [0-9]+' "$GRADLE_FILE" | tr -dc '0-9')
if [ -n "$CURRENT_VERSION_CODE" ]; then
    NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
    sed -i "s/versionCode = $CURRENT_VERSION_CODE/versionCode = $NEW_VERSION_CODE/" "$GRADLE_FILE"
    echo "Updated versionCode to $NEW_VERSION_CODE"
fi

# Update versionName
sed -i "s/versionName = \".*\"/versionName = \"$NEW_VERSION\"/" "$GRADLE_FILE"
echo "Updated versionName to $NEW_VERSION"

# Commit and tag changes
git add "$GRADLE_FILE"
git commit -m "build: update version to ${NEW_VERSION}+${NEW_VERSION_CODE}"
git tag "v${NEW_VERSION}"

# Capitalize flavor for gradle task (e.g., prod -> Prod)
FLAVOR_CAP=$(echo "${FLAVOR:0:1}" | tr '[:lower:]' '[:upper:]')${FLAVOR:1}
TASK="assemble${FLAVOR_CAP}Release"

echo "Running ./gradlew $TASK..."
./gradlew "$TASK"

# Find the generated APK
APK_DIR="composeApp/build/outputs/apk/${FLAVOR}/release"
APK_FILE=$(find "$APK_DIR" -name "*.apk" | head -n 1)

if [ -z "$APK_FILE" ]; then
    echo "Error: APK not found in $APK_DIR"
    exit 1
fi

NEW_APK_NAME="${PACKAGE_NAME}-${NEW_VERSION}-${FLAVOR}.apk"

mkdir -p "$OUTPUT_DIR"
cp "$APK_FILE" "$OUTPUT_DIR/$NEW_APK_NAME"

echo "Successfully built and copied APK to $OUTPUT_DIR/$NEW_APK_NAME"
