# ---- Build ----
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline || true
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests package

# ---- Runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home appuser
COPY --from=build /build/target/digital-student-card.jar app.jar
USER appuser
EXPOSE 8080
# Java2D (photo resizing, demo pictures) and PDF building run without a screen.
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true -XX:MaxRAMPercentage=75"
ENTRYPOINT ["java","-jar","app.jar"]
