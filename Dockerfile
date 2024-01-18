FROM openjdk:17-slim
EXPOSE 8000
VOLUME /app
ADD target/original-my-vertx-project-1.0-SNAPSHOT.jar app.jar

RUN apt-get update && \
    apt-get install -y postgresql-client

ENV POSTGRES_USER postgres
ENV POSTGRES_PASSWORD admin
ENV POSTGRES_DB postgres
ENV POSTGRES_HOST postgres
ENV POSTGRES_PORT 5432


CMD ["java", "-jar", "/app.jar"]

