FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S icop && adduser -S icop -G icop
WORKDIR /app
COPY target/*.jar app.jar
RUN chown icop:icop app.jar
USER icop
EXPOSE 8085
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
