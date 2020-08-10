FROM iofog/ubuntu-16.04-java8 AS builder

RUN apt-get update && \
    apt-get install -y --no-install-recommends unzip && \
    apt-get install -y apt-utils curl && \
    apt-get clean

COPY . .

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

RUN gradle build copyJar -x test

FROM jpetazzo/dind

COPY --from=builder packaging/iofog-agent/etc /etc
COPY --from=builder   packaging/iofog-agent/usr /usr

RUN cd /opt && \
  curl -SL http://www.edgeworx.io/downloads/jdk/jdk-8u211-64.tar.gz \
  | tar -xzC /opt && \
  update-alternatives --install /usr/bin/java java /opt/jdk1.8.0_211/bin/java 1100

RUN apt-get update && \
    apt-get install -y sudo && \
    useradd -r -U -s /usr/bin/nologin iofog-agent && \
    usermod -aG root,sudo iofog-agent && \
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
    mv /dev/random /dev/random.real && \
    ln -s /dev/urandom /dev/random && \
    chmod 774 /etc/init.d/iofog-agent && \
    chmod 754 /usr/bin/iofog-agent && \
    chown :iofog-agent /usr/bin/iofog-agent && \
    update-rc.d iofog-agent defaults && \
    ln -sf /usr/bin/iofog-agent /usr/local/bin/iofog-agent && \
    echo "service iofog-agent start && tail -f /dev/null" >> /start.sh
CMD [ "sh", "/start.sh" ]