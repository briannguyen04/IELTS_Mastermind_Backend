# Use stable Java (LTS)
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy all files
COPY . .

# Build the app
RUN ./mvnw clean package -DskipTests

# Expose port (Render uses PORT env anyway)
EXPOSE 8080

# Run the app
CMD ["sh", "-c", "java -jar target/*.jar"]