FROM jpetazzo/dind

RUN cd /opt && \
  curl -SL http://www.edgeworx.io/downloads/jdk/jdk-8u211-64.tar.gz \
  | tar -xzC /opt && \
  update-alternatives --install /usr/bin/java java /opt/jdk1.8.0_211/bin/java 1100

COPY packaging/iofog-agent/etc /etc
COPY packaging/iofog-agent/usr /usr

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
