# Build from repository root:
#   docker build -f backend/Dockerfile -t roleplay-backend .

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY backend/pom.xml .
COPY backend/src ./src

RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /build/target/quarkus-app/ ./
COPY data /data

RUN mkdir -p /data/conversations

ENV ROLEPLAY_DATA_DIR=/data
ENV PORT=8080

EXPOSE 8080

CMD ["java", "-jar", "quarkus-run.jar"]
