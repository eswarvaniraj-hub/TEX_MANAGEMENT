# ==============================================================================
# Stage 1: Build React + Vite + Mantine Frontend
# ==============================================================================
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# ==============================================================================
# Stage 2: Build Spring Boot Backend (Java 21)
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS backend-builder
WORKDIR /app

COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY backend/src ./src

# Embed production frontend bundle into Spring Boot static web resources
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static

RUN ./mvnw clean package -DskipTests -B

# ==============================================================================
# Stage 3: Minimal, Secure Production Runtime Container
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=backend-builder /app/target/*.jar app.jar

USER appuser

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
