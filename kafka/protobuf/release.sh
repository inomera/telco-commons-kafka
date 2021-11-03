#!/bin/bash
set -e

TAG_PREFIX="kafka-protobuf"

# ensure we're up to date
git pull

# Validate release type parameter
RELEASE_TYPE=""
if [[ "${1}" = "major" ]] || [[ "${1}" = "minor" ]] || [[ "${1}" = "patch" ]]; then
  RELEASE_TYPE="${1}"
elif [[ ${1} = "" ]]; then
  RELEASE_TYPE="patch"
else
  echo "Illegal argument: ${1}"
  echo "Usage: ./release.sh [patch|minor|major]"
  exit 1
fi

echo "${RELEASE_TYPE}"

forceNewVersion="false"

until [[ "$#" == "0" ]]; do
    case "$1" in
        --force-new-version )
            forceNewVersion="true"
            ;;
    esac

    shift
done

echo "forceNewVersion=${forceNewVersion}"

# Check if the current commit is already versioned
previousVersion=$(cat VERSION)
alreadyVersioned=$(git tag -l --points-at HEAD "${TAG_PREFIX}/${previousVersion}")
if [[ "${alreadyVersioned}" != "" ]]; then
  if [[ "${forceNewVersion}" = "false" ]]; then
      echo "Current HEAD already contains version tag ${TAG_PREFIX}/${alreadyVersioned}"
      echo "Ignoring release command."
      exit 1
  fi
fi

# bump (increment) version
docker run --rm -v "$PWD":/app treeder/bump "${RELEASE_TYPE}"
version=`cat VERSION`
echo "Version: ${version}"

# run build & tests
../../gradlew clean build

# tag it
git add -A
git commit -m "Release version ${TAG_PREFIX}/${version}"
git tag -a "${TAG_PREFIX}/${version}" -m "Version ${TAG_PREFIX}/${version}"
git push
git push --tags

# publish it
../../gradlew :kafka-protobuf:publish
