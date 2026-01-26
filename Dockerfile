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

# Copy BOTH CSS and Templates so Tailwind can scan the HTML
COPY src/main/resources/static/css ./src/main/resources/static/css
COPY src/main/resources/templates ./src/main/resources/templates

# Build CSS
RUN npm run build

# ==========================================
# STAGE 2: Backend Build (Java 25)
# ==========================================
FROM eclipse-temurin:25-jdk-alpine AS backend
WORKDIR /app

# Copy Maven wrapper and configuration
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copy Source Code
COPY src ./src

# Copy compiled CSS from Frontend Stage
COPY --from=frontend /app/src/main/resources/static/dist ./src/main/resources/static/dist

# Build and Extract Layers (Crucial for CDS)
RUN ./mvnw clean package -DskipTests && \
    java -Djarmode=layertools -jar target/*.jar extract

# ==========================================
# STAGE 3: Production Runtime
# ==========================================
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy Layers (Better caching than fat jar)
COPY --from=backend /app/dependencies/ ./
COPY --from=backend /app/spring-boot-loader/ ./
COPY --from=backend /app/snapshot-dependencies/ ./
COPY --from=backend /app/application/ ./

# Generate CDS Archive (Boosts startup by ~30%)
RUN java -XX:ArchiveClassesAtExit=application.jsa \
    -Dspring.context.exit=onRefresh \
    -XX:TieredStopAtLevel=1 \
    org.springframework.boot.loader.launch.JarLauncher || true

USER spring:spring

# 4. Set environment - Production Profile
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
EXPOSE 8080

# FINAL COMMAND: Uses CDS + TieredStop
ENTRYPOINT ["java", "-XX:SharedArchiveFile=application.jsa", "-XX:TieredStopAtLevel=1", "-Xmx256m", "-Xss512k", "org.springframework.boot.loader.launch.JarLauncher"]