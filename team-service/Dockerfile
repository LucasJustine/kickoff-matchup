FROM openjdk:17-alpine
LABEL authors="Lucas"

ADD target/team-service-0.0.1-SNAPSHOT.jar team-service.jar

ENTRYPOINT ["java", "-jar", "/team-service.jar"]