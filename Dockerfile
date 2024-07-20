# Use the official OpenJDK image as a base
FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the built JAR file into the container
COPY target/stock-price-analytics-0.0.1-SNAPSHOT.jar app.jar

# Specify the command to run the application
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
