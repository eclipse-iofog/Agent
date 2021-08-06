FROM docker.io/library/ubuntu:20.04 AS builder

RUN apt-get update && \
    apt-get install -y unzip apt-utils curl openjdk-8-jdk && \
    apt-get clean

# 1- Define a constant with the version of gradle you want to install
ARG GRADLE_VERSION=5.4

# 2- Define the URL where gradle can be downloaded from
ARG GRADLE_BASE_URL=https://services.gradle.org/distributions

# 3- Define the SHA key to validate the gradle download
#    obtained from here https://gradle.org/release-checksums/
ARG GRADLE_SHA=c8c17574245ecee9ed7fe4f6b593b696d1692d1adbfef425bef9b333e3a0e8de

# 4- Create the directories, download gradle, validate the download, install it, remove downloaded file and set links
RUN mkdir -p /usr/share/gradle /usr/share/gradle/ref \
  && echo "Downlaoding gradle hash" \
  && curl -fsSL -o /tmp/gradle.zip ${GRADLE_BASE_URL}/gradle-${GRADLE_VERSION}-bin.zip \
  \
  && echo "Checking download hash" \
  && echo "${GRADLE_SHA}  /tmp/gradle.zip" | sha256sum -c - \
  \
  && echo "Unziping gradle" \
  && unzip -d /usr/share/gradle /tmp/gradle.zip \
   \
  && echo "Cleaning and setting links" \
  && rm -f /tmp/gradle.zip \
  && ln -s /usr/share/gradle/gradle-${GRADLE_VERSION} /usr/bin/gradle

# 5- Define environmental variables required by gradle
ENV GRADLE_VERSION 5.4
ENV GRADLE_HOME /usr/bin/gradle
ENV GRADLE_USER_HOME /cache
ENV PATH $PATH:$GRADLE_HOME/bin

VOLUME $GRADLE_USER_HOME

COPY . .

RUN gradle build copyJar -x test --no-daemon

FROM registry.access.redhat.com/ubi8/ubi-minimal:latest

RUN true && \
    microdnf install -y curl ca-certificates java-11-openjdk-headless sudo shadow-utils && \
    microdnf clean all && \
    true

RUN echo "securerandom.source=file:/dev/urandom" >> /etc/alternatives/jre/lib/security/java.security

COPY --from=builder packaging/iofog-agent/etc /etc
COPY --from=builder packaging/iofog-agent/usr /usr

RUN true && \
    useradd -r -U -s /usr/bin/nologin iofog-agent && \
    usermod -aG root,wheel iofog-agent && \
    mv /etc/iofog-agent/config_new.xml /etc/iofog-agent/config.xml && \
    mv /etc/iofog-agent/config-development_new.xml /etc/iofog-agent/config-development.xml && \
    mv /etc/iofog-agent/config-production_new.xml /etc/iofog-agent/config-production.xml && \
    mv /etc/iofog-agent/config-switcher_new.xml /etc/iofog-agent/config-switcher.xml && \
    mv /etc/iofog-agent/cert_new.crt /etc/iofog-agent/cert.crt && \
    </dev/urandom tr -dc A-Za-z0-9 | head -c32 > /etc/iofog-agent/local-api && \
    mkdir -p /var/backups/iofog-agent && \
    mkdir -p /var/log/iofog-agent && \
    mkdir -p /var/lib/iofog-agent && \
    mkdir -p /var/run/iofog-agent && \
    chown -R :iofog-agent /etc/iofog-agent && \
    chown -R :iofog-agent /var/log/iofog-agent && \
    chown -R :iofog-agent /var/lib/iofog-agent && \
    chown -R :iofog-agent /var/run/iofog-agent && \
    chown -R :iofog-agent /var/backups/iofog-agent && \
    chown -R :iofog-agent /usr/share/iofog-agent && \
    chmod 774 -R /etc/iofog-agent && \
    chmod 774 -R /var/log/iofog-agent && \
    chmod 774 -R /var/lib/iofog-agent && \
    chmod 774 -R /var/run/iofog-agent && \
    chmod 774 -R /var/backups/iofog-agent && \
    chmod 754 -R /usr/share/iofog-agent && \
    chmod 774 /etc/init.d/iofog-agent && \
    chmod 754 /usr/bin/iofog-agent && \
    chown :iofog-agent /usr/bin/iofog-agent && \
    true

CMD [ "java", "-jar", "/usr/bin/iofog-agentd.jar", "start" ]
