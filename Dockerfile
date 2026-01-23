# ==========================================
# STAGE 1: Frontend Build (Tailwind/Vite)
# ==========================================
FROM node:20-alpine AS frontend
WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy configuration
COPY vite.config.js postcss.config.mjs ./

# ✅ Copy BOTH CSS and Templates so Tailwind can scan the HTML
COPY src/main/resources/static/css ./src/main/resources/static/css
COPY src/main/resources/templates ./src/main/resources/templates

# Build CSS (Now it will see your HTML classes!)
RUN npm run build

# ==========================================
# STAGE 2: Backend Build (Java/Maven)
# ==========================================
FROM eclipse-temurin:25-jdk-noble AS backend
WORKDIR /app

# Copy Maven wrapper and configuration
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy Source Code
COPY src ./src

# Copy compiled CSS from Frontend Stage
COPY --from=frontend /app/src/main/resources/static/dist ./src/main/resources/static/dist

# Build the JAR
RUN ./mvnw clean package -DskipTests

# ==========================================
# STAGE 3: Production Runtime
# ==========================================
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# 1. Install System Deps
RUN apt-get update && \
    apt-get install -y curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# 2. Create app directory
RUN groupadd -r spring && useradd -r -g spring spring && \
    chown -R spring:spring /app

# 3. Copy Application
COPY --from=backend --chown=spring:spring /app/target/*.jar app.jar
USER spring:spring

# 4. Set environment - Production Profile
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
EXPOSE 8080

# ✅ FIXED: Added Heap Limits to prevent OOM in 512MB Container
# -Xmx300m: Max Heap 300MB (leaves ~200MB for OS overhead)
ENTRYPOINT ["java", "-Xmx300m", "-Xss512k", "-jar", "app.jar"]