# ---------- Stage 1: build the jar with Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# copy pom.xml first and download dependencies, so Docker can cache this layer
COPY pom.xml .
RUN mvn -B dependency:go-offline

# now copy the source code and build (tests are skipped here, run them with: mvn test)
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: small runtime image, only a JRE ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# copy only the finished jar from the build stage
COPY --from=build /app/target/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
