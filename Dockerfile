FROM --platform=$BUILDPLATFORM maven:3.8.4-jdk-8-slim AS build

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install --no-install-recommends -y \
        git procps-ng && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /src

COPY dhis-2 .

RUN mvn clean install -T1C -f pom.xml -DskipTests
RUN mvn clean install -T1C -U -f dhis-web/pom.xml -DskipTests

FROM tomcat:9-jdk8-corretto

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /src/dhis-web/dhis-web-portal/target/dhis.war /usr/local/tomcat/webapps/ROOT.war
COPY dhis.conf /opt/dhis2/dhis.conf

ENV JAVA_OPTS="-Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -Xss512k -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:MaxRAMPercentage=80.0 -Xloggc:/dev/stdout XX:+PrintGC -XX:+PrintGCDateStamps"

CMD ["catalina.sh", "run"]
