# ===== 构建阶段 =====
FROM maven:3.9-eclipse-temurin-22 AS build
WORKDIR /app
COPY campus-trade-server/pom.xml .
RUN mvn dependency:go-offline -B
COPY campus-trade-server/src ./src
RUN mvn package -DskipTests -B

# ===== 运行阶段 =====
FROM eclipse-temurin:22-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
