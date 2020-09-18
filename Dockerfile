FROM openjdk:14-jdk-slim-buster

MAINTAINER jHapy Lead Dev <jhapy@jhapy.org>

RUN apt-get update -y && \
    apt-get install -y wget curl libxinerama1 libdbus-1-3 libgio-cil libcairo2 cups libsm6 libx11-xcb1

RUN cd /tmp && \
    wget https://download.documentfoundation.org/libreoffice/stable/7.0.1/deb/x86_64/LibreOffice_7.0.1_Linux_x86-64_deb.tar.gz && \
    tar -xvzf LibreOffice_7.0.1_Linux_x86-64_deb.tar.gz && \
    cd LibreOffice_7.0.1.2_Linux_x86-64_deb/DEBS/ && \
    rm libobasis7.0-gnome-integration_7.0.1.2-2_amd64.deb libobasis7.0-kde-integration_7.0.1.2-2_amd64.deb libreoffice7.0-debian-menus_7.0.1-2_all.deb  && \
    apt-get install ./*.deb && \
    cd ../.. && \
    rm -rf LibreOffice_7.0.1*

RUN apt-get autoclean

ENV JAVA_OPTS=""
ENV APP_OPTS=""

ADD devgcp.crt /tmp/
RUN $JAVA_HOME/bin/keytool -importcert -file /tmp/devgcp.crt -alias devgcp -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt
ADD ilemtest.crt /tmp/
RUN $JAVA_HOME/bin/keytool -importcert -file /tmp/ilemtest.crt -alias ilemtest -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt

ADD target/app-resource-server.jar /app/

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dpinpoint.agentId=$(date | md5sum | head -c 24) -jar /app/app-resource-server.jar $APP_OPTS"]

HEALTHCHECK --interval=30s --timeout=30s --retries=10 CMD curl -f http://localhost:9105/management/health || exit 1

EXPOSE 9005 9105