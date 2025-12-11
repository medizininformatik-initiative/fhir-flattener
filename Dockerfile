# syntax=docker/dockerfile:1
FROM gradle:8.14-jdk21 AS temp_build_image

COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src

RUN gradle clean shadowJar --no-daemon

FROM eclipse-temurin:21
ENV ARTIFACT_NAME=fhir-flattener.jar

WORKDIR /app
COPY --from=temp_build_image /home/gradle/src/build/libs/* .

ENTRYPOINT exec java --add-exports java.base/sun.nio.ch=ALL-UNNAMED -Xmx16g -jar ${ARTIFACT_NAME}