# syntax=docker/dockerfile:1@sha256:572cebbc2b15e6f84c077742952674889cb63cd5e9b80abdffcbe587b6f19183
# The jar does not depend on the platform, so build it natively on the build host.
# Some gradle images (for example 9.9.2-jdk21) have no arm64 variant.
FROM --platform=$BUILDPLATFORM gradle:9.9.2-jdk21@sha256:f8c4f7de6ec4f367cbf7fc46db08129ea6a6e9487995bb705c61fbc20f3bc545 AS temp_build_image

COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src

RUN gradle clean shadowJar --no-daemon

# Spark/Hadoop (via Pathling) does not start on Java 25: Subject.getSubject is not supported.
FROM eclipse-temurin:21@sha256:8ad118b146aa92da815b576441acd6b42b38236797ad0b6edc8c200b85bb0734
ENV ARTIFACT_NAME=fhir-flattener.jar

WORKDIR /app
COPY --from=temp_build_image /home/gradle/src/build/libs/* .

ENTRYPOINT exec java --add-exports java.base/sun.nio.ch=ALL-UNNAMED -Xmx16g -jar ${ARTIFACT_NAME}