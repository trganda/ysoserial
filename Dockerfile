FROM maven:3.8.6-openjdk-8 as builder

WORKDIR /app

# download artifacts
COPY pom.xml .
COPY assembly.xml .
RUN mvn dependency:resolve

# build
COPY src ./src
RUN mvn clean package -DskipTests
RUN mv target/ysoserial-*all*.jar target/ysoserial.jar

FROM openjdk:8-jdk-alpine

WORKDIR /app

COPY --from=builder /app/target/ysoserial.jar .

ENTRYPOINT ["java", "-jar", "ysoserial.jar"]
