#!/usr/bin/env bash
set -e

GRADLE_FILE="composeApp/build.gradle.kts"
BUMP=$1

if ! [[ $BUMP =~ ^(major|minor|patch)$ ]]; then
    echo "Usage: $0 [major|minor|patch]"
    exit 1
fi

if ! git diff --quiet HEAD -- "$GRADLE_FILE"; then
    echo "Error: $GRADLE_FILE has uncommitted changes."
    exit 1
fi

CURRENT_NAME=$(grep -m 1 'appVersionName =' "$GRADLE_FILE" | cut -d '"' -f 2)
CURRENT_CODE=$(grep -m 1 'appVersionCode =' "$GRADLE_FILE" | tr -dc '0-9')

if ! [[ $CURRENT_NAME =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || [ -z "$CURRENT_CODE" ]; then
    echo "Error: could not read a MAJOR.MINOR.PATCH appVersionName and an appVersionCode from $GRADLE_FILE"
    exit 1
fi

IFS=. read -r MAJOR MINOR PATCH <<< "$CURRENT_NAME"
case $BUMP in
    major) NEW_NAME="$((MAJOR + 1)).0.0" ;;
    minor) NEW_NAME="$MAJOR.$((MINOR + 1)).0" ;;
    patch) NEW_NAME="$MAJOR.$MINOR.$((PATCH + 1))" ;;
esac
NEW_CODE=$((CURRENT_CODE + 1))
TAG="v$NEW_NAME"

if git rev-parse -q --verify "refs/tags/$TAG" > /dev/null; then
    echo "Error: tag $TAG already exists."
    exit 1
fi

sed -i.bak \
    -e "s/\(appVersionName = \)\"[^\"]*\"/\1\"$NEW_NAME\"/" \
    -e "s/\(appVersionCode = \)[0-9]*/\1$NEW_CODE/" \
    "$GRADLE_FILE"
rm "$GRADLE_FILE.bak"

git add "$GRADLE_FILE"
git commit -m "build: update version to ${NEW_NAME}+${NEW_CODE}"
git tag "$TAG"

echo "Bumped $CURRENT_NAME+$CURRENT_CODE -> $NEW_NAME+$NEW_CODE"