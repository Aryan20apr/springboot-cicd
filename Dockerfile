FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

ARG GIT_REVISION=unknown
ARG BUILD_DATE=unknown

LABEL org.opencontainers.image.revision="${GIT_REVISION}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.source="https://github.com/Aryan20apr/springboot-cicd"

# JAR is pre-built by the CI 'build' job and downloaded into target/ before
# docker build runs. This ensures the tested artifact is the deployed artifact,
# and that git.properties (with commit SHA) is already embedded.
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]