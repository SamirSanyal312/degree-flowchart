# Degree Flowchart

A small Spring Boot web app that renders an MS-CS degree flow (courses, prerequisites, and semester planning).
Backed by SQLite; UI rendered with Thymeleaf and vanilla JS.

# Tech stack

Java 21, Spring Boot 3, Spring Data JDBC

SQLite (file-based)

Maven Wrapper (mvnw)

Docker (for container builds)

GitHub Actions + GitHub Container Registry (GHCR) + Render

Quick start (local)

Requirements: JDK 21, Git.

# Clone
git clone https://github.com/<your-user>/degree-flowchart.git
cd degree-flowchart

run in dev (uses src/main/resources/application.properties)

./mvnw spring-boot:run

app: http://localhost:8080

Run with SQLite file and prod profile
SPRING_PROFILES_ACTIVE=prod \
DB_PATH=./planner.db \
SERVER_PORT=8080 \
./mvnw spring-boot:run

# Tests and build
# unit tests
./mvnw -B -ntp test

# build jar (target/*.jar)
./mvnw -DskipTests package

# Docker
Build and run container (bind-mount a host folder for the DB):
# build image
docker build -t ghcr.io/<your-user>/degree-flowchart:latest .

# run
mkdir -p _data
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_PATH=/data/planner.db \
  -v "$(pwd)"/_data:/data \
  ghcr.io/<your-user>/degree-flowchart:latest

Configuration

# Environment variables:

SPRING_PROFILES_ACTIVE — dev or prod

DB_PATH — filesystem path to the SQLite DB file (required in prod)

SERVER_PORT — optional, defaults to 8080

SQL schema and seed data live in schema.sql and data.sql and are auto-applied at startup.

# CI/CD

GitHub Actions workflow:

runs unit tests with mvnw

builds a multi-arch Docker image with Buildx

pushes to ghcr.io/<your-user>/degree-flowchart:latest

triggers a Render deploy hook to roll out the new image

Production runs on Render as a Docker-based Web Service with:

SPRING_PROFILES_ACTIVE=prod

DB_PATH=/data/planner.db

a persistent disk mounted at /data

# Project structure
src/
  main/
    java/...             # controllers, services, repositories
    resources/
      templates/         # Thymeleaf views
      static/            # CSS/JS
      application.properties
      application-prod.properties
schema.sql
data.sql
Dockerfile

# License

MIT © 2025 Samir Sanyal. See [LICENSE](./LICENSE) for details.