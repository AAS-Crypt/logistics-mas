# Logistics MAS - Docker Image
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY lib/ ./lib/
COPY src/ ./src/
COPY bin/ ./bin/
COPY src/main/resources/config.properties ./config.properties
COPY spec.md ./spec.md

EXPOSE 1099

ENV CLASSPATH="bin:lib/*"

CMD ["java", "-cp", "bin:lib/*", "com.logistics.simulator.LogisticsSimulator", "10", "3", "5"]