FROM openjdk:14-jdk-oracle

MAINTAINER jHapy Lead Dev <jhapy@jhapy.org>

RUN apt-get update -y && \
    apt-get install -y wget dbus-libs cairo cups curl

RUN cd /tmp && \
    wget https://download.documentfoundation.org/libreoffice/stable/7.0.1/rpm/x86_64/LibreOffice_7.0.1_Linux_x86-64_rpm.tar.gz && \
    tar -xvzf LibreOffice_7.0.1_Linux_x86-64_rpm.tar.gz && \
    cd LibreOffice_7.0.1.2_Linux_x86-64_rpm/RPMS/ && \
    rm libobasis7.0-gnome-integration-7.0.1.2-2.x86_64.rpm libobasis7.0-kde-integration-7.0.1.2-2.x86_64.rpm libreoffice7.0-freedesktop-menus-7.0.1-2.noarch.rpm && \
    yum -y localinstall *.rpm && \
    cd ../.. && \
    rm -rf LibreOffice_7.0.1*

RUN apt-get clean all

ENV JAVA_OPTS=""
ENV APP_OPTS=""

ADD devgcp.crt /tmp/
RUN $JAVA_HOME/bin/keytool -importcert -file /tmp/devgcp.crt -alias devgcp -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt

ADD target/app-resource-server.jar /app/

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app-resource-server.jar $APP_OPTS"]

HEALTHCHECK --interval=30s --timeout=30s --retries=10 CMD curl -f http://localhost:9105/management/health || exit 1

EXPOSE 9005 9105