#
# Copyright Kroxylicious Authors.
#
# Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
#

FROM registry.access.redhat.com/ubi9/openjdk-17:1.20 AS builder

ARG TARGETOS
ARG TARGETARCH

USER root
WORKDIR /opt/kroxylicious
COPY . .
RUN mvn -q -B clean package -Pdist -Dquick

FROM quay.io/sbarker/tini:${TARGETARCH}_0.19.0-1 AS tini
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.2

ARG JAVA_VERSION=17
ARG KROXYLICIOUS_VERSION
ARG CONTAINER_USER=kroxylicious
ARG CONTAINER_USER_UID=185

USER root

RUN microdnf -y update \
    && microdnf --setopt=install_weak_deps=0 --setopt=tsflags=nodocs install -y \
                java-${JAVA_VERSION}-openjdk-headless \
                openssl \
                shadow-utils \
    && if [[ -n "${CONTAINER_USER}" && "${CONTAINER_USER}" != "root" ]] ; then groupadd -r -g "${CONTAINER_USER_UID}" "${CONTAINER_USER}" && useradd -m -r -u "${CONTAINER_USER_UID}" -g "${CONTAINER_USER}" "${CONTAINER_USER}"; fi \
    && microdnf remove -y shadow-utils \
    && microdnf clean all

ENV JAVA_HOME=/usr/lib/jvm/jre-17

COPY --from=tini /opt/tini/bin/tini /usr/bin/tini
COPY --from=builder /opt/kroxylicious/kroxylicious-app/target/kroxylicious-app-${KROXYLICIOUS_VERSION}-bin/kroxylicious-app-${KROXYLICIOUS_VERSION}/ /opt/kroxylicious/

USER ${CONTAINER_USER_UID}

ENTRYPOINT ["/usr/bin/tini", "--", "/opt/kroxylicious/bin/kroxylicious-start.sh" ]