FROM openjdk:12.0.2

EXPOSE 8761

ADD ./target/eureka-server-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]