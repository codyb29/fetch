# syntax=docker/dockerfile:1
FROM openjdk:17
COPY * /
RUN javac -cp jsoup-1.14.3.jar Fetch.java
ENTRYPOINT ["java", "-cp", "jsoup-1.14.3.jar", "Fetch.java"]
