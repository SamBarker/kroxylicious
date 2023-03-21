#!/usr/bin/env bash

#
# Copyright Kroxylicious Authors.
#
# Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
#

RELEASE_VERSION=${1:-0.1.0}
RELEASE_API_VERSION=${2:-0.1.0}
API_MODULES=':kroxylicious-api,:kroxylicious-filter-api'

if [[ -z "${GPG_KEY}" ]]; then
    echo "GPG_KEY not set unable to sign the release. Please export GPG_KEY" 1>&2
    exit 1
fi

echo "Validating the build is green"
mvn clean install || { echo 'maven build failed' ; exit 1; }
echo "Releasing API version ${RELEASE_API_VERSION} as part of version ${RELEASE_VERSION}"
mvn versions:set -DnewVersion="${RELEASE_API_VERSION}" -DprocessAllModules=true -pl ${API_MODULES} -DgenerateBackupPoms=false || { echo 'failed to set the API version' ; exit 1; }
mvn clean install -Pquick -pl ${API_MODULES}
mvn versions:set-property -Dproperty=kroxyliciousApi.version -DnewVersion="${RELEASE_API_VERSION}" -DgenerateBackupPoms=false || { echo "failed to depend on API version ${RELEASE_API_VERSION}" ; exit 1; }
mvn versions:set -DnewVersion="${RELEASE_VERSION}" -pl '!:kroxylicious-api,!:kroxylicious-filter-api'  -DgenerateBackupPoms=false || { echo 'failed to set the release version' ; exit 1; }
echo "Validating things still build"
mvn clean install -Pquick

echo "Committing release to git"
git add '**/pom.xml' 'pom.xml'
git commit --message "Release version v${RELEASE_VERSION}" --signoff
git tag "api-v${RELEASE_API_VERSION}"
git tag "v${RELEASE_VERSION}"

git push "${PUSH_REMOTE:-origin}" --tags

echo "Deploying release to maven central"
mvn deploy -Prelease -DskipTests=true -DreleaseSigningKey="${GPG_KEY}"

if ! command -v gh &> /dev/null
then
    echo "gh command could not be found. Please create a pull request by hand https://github.com/kroxylicious/kroxylicious/compare"
    exit
fi

gh pr create --base main --title "Release v${RELEASE_VERSION}" --body "Release v${RELEASE_VERSION} which includes API version ${RELEASE_API_VERSION}"
