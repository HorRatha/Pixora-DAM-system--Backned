FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY . .

RUN apt-get update && apt-get install -y maven

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/digital-asset-management-1.0.0.jar"]