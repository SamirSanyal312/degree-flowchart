# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests package

# ---- Run stage ----
FROM eclipse-temurin:21-jre
# Non-root user
RUN useradd -ms /bin/bash appuser
WORKDIR /app

# SQLite lives here (mapped to a volume/disk)
RUN mkdir -p /data && chown -R appuser:appuser /data
VOLUME ["/data"]

# Copy fat jar
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar
USER appuser

EXPOSE 8080
ENV JAVA_OPTS=""
# Use prod profile by default; bind to $PORT if present
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]