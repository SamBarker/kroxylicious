#
# Copyright Kroxylicious Authors.
#
# Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
#

FROM registry.access.redhat.com/hi/openjdk:21.0.11-runtime-builder@sha256:c9ebda3dfc26f258cd39647cf41f5204a98ff3305950baf2cccab7e524621a98 AS setup

ARG TARGETOS=linux
ARG TARGETARCH
ARG CONTAINER_USER=kroxylicious
ARG CONTAINER_USER_UID=185

USER root

RUN dnf5 install -y curl \
    && dnf5 clean all

# Download Tini
ARG TINI_VERSION=v0.19.0
ARG TINI_SHA256_AMD64=93dcc18adc78c65a028a84799ecf8ad40c936fdfc5f2a57b1acda5a8117fa82c
ARG TINI_SHA256_ARM64=07952557df20bfd2a95f9bef198b445e006171969499a1d361bd9e6f8e5e0e81
ARG TINI_SHA256_PPC64LE=3f658420974768e40810001a038c29d003728c5fe86da211cff5059e48cfdfde
ARG TINI_SHA256_S390X=931b70a182af879ca249ae9de87ef68423121b38d235c78997fafc680ceab32d
ARG TINI_DEST=/usr/bin/tini

RUN set -ex; \
    mkdir -p /opt/tini/bin/; \
    if [[ "${TARGETOS}/${TARGETARCH}" = "linux/ppc64le" ]]; then \
        curl -s -L https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini-ppc64le -o ${TINI_DEST}; \
        echo "${TINI_SHA256_PPC64LE} *${TINI_DEST}" | sha256sum -c; \
    elif [[ "${TARGETOS}/${TARGETARCH}" = "linux/arm64" ]]; then \
        curl -s -L https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini-arm64 -o ${TINI_DEST}; \
        echo "${TINI_SHA256_ARM64} *${TINI_DEST}" | sha256sum -c; \
    elif [[ "${TARGETOS}/${TARGETARCH}" = "linux/s390x" ]]; then \
        curl -s -L https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini-s390x -o ${TINI_DEST}; \
        echo "${TINI_SHA256_S390X} *${TINI_DEST}" | sha256sum -c; \
    else \
        curl -s -L https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini -o ${TINI_DEST}; \
        echo "${TINI_SHA256_AMD64} *${TINI_DEST}" | sha256sum -c; \
    fi; \
    chmod +x ${TINI_DEST}

RUN groupadd -r -g "${CONTAINER_USER_UID}" "${CONTAINER_USER}" \
    && useradd -m -r -u "${CONTAINER_USER_UID}" -g "${CONTAINER_USER}" "${CONTAINER_USER}"

FROM registry.access.redhat.com/hi/openjdk:21.0.11-runtime-builder@sha256:c9ebda3dfc26f258cd39647cf41f5204a98ff3305950baf2cccab7e524621a98

ARG KROXYLICIOUS_VERSION
ARG CONTAINER_USER=kroxylicious
ARG CONTAINER_USER_UID=185

USER root

COPY --from=setup /etc/passwd /etc/passwd
COPY --from=setup /etc/group /etc/group
COPY --from=setup /home/kroxylicious /home/kroxylicious
COPY --from=setup /usr/bin/tini /usr/bin/tini

WORKDIR /opt/kroxylicious-operator

COPY target/kroxylicious-operator-${KROXYLICIOUS_VERSION}-app/kroxylicious-operator-${KROXYLICIOUS_VERSION}/ .

USER ${CONTAINER_USER_UID}

ENTRYPOINT ["/usr/bin/tini", "--", "/opt/kroxylicious-operator/bin/operator-start.sh" ]

LABEL url="https://kroxylicious.io/"
LABEL vendor="The Kroxylicious Project"
LABEL maintainer="Kroxylicious Maintainers"
