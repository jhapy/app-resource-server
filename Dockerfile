FROM openjdk:14-jdk-oracle

MAINTAINER jHapy Lead Dev <jhapy@jhapy.org>

RUN yum update -y && \
    yum clean all

ENV JAVA_OPTS=""
ENV APP_OPTS=""

ADD devgcp.crt /tmp/
RUN $JAVA_HOME/bin/keytool -importcert -file /tmp/devgcp.crt -alias devgcp -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt

ADD target/app-resource-server.jar /app/

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app-resource-server.jar $APP_OPTS"]

HEALTHCHECK --interval=30s --timeout=30s --retries=10 CMD curl -f http://localhost:9105/management/health || exit 1

EXPOSE 9005 9105