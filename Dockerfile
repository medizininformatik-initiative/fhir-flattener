# syntax=docker/dockerfile:1
# The jar does not depend on the platform, so build it natively on the build host.
# Some gradle images (for example 9.9.2-jdk21) have no arm64 variant.
FROM --platform=$BUILDPLATFORM gradle:9.9.2-jdk21 AS temp_build_image

COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src

RUN gradle clean shadowJar --no-daemon

# Spark/Hadoop (via Pathling) does not start on Java 25: Subject.getSubject is not supported.
FROM eclipse-temurin:21
ENV ARTIFACT_NAME=fhir-flattener.jar

WORKDIR /app
COPY --from=temp_build_image /home/gradle/src/build/libs/* .

ENTRYPOINT exec java --add-exports java.base/sun.nio.ch=ALL-UNNAMED -Xmx16g -jar ${ARTIFACT_NAME}