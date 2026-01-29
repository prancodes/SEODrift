# ==========================================
# STAGE 1: Frontend Build (Tailwind/Vite)
# ==========================================
FROM node:20-alpine AS frontend
WORKDIR /app

# Cache dependencies first to speed up re-builds
COPY package*.json ./
RUN npm ci --silent

# Copy config and source for build
COPY vite.config.js postcss.config.mjs ./
COPY src/main/resources/static/css ./src/main/resources/static/css
COPY src/main/resources/templates ./src/main/resources/templates

# Build Tailwind/CSS (Output: src/main/resources/static/dist)
RUN npm run build

# ==========================================
# STAGE 2: Backend Build & JLink (Java 25)
# ==========================================
FROM eclipse-temurin:25-jdk-alpine AS backend
WORKDIR /app

# 1. Cache Maven Dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# 2. Build Application
COPY src ./src
# Bring in the compiled frontend assets from Stage 1
COPY --from=frontend /app/src/main/resources/static/dist ./src/main/resources/static/dist

# Package and Extract Layers (enables Docker caching for dependencies)
RUN ./mvnw clean package -DskipTests && \
    java -Djarmode=layertools -jar target/*.jar extract

# 3. Create Minimal Java Runtime (JLink)
#    This creates a custom JRE with ONLY the modules Spring Boot needs
RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.logging,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.unsupported,java.sql,java.net.http,java.scripting,java.xml,jdk.jfr,jdk.crypto.ec \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /javaruntime

# ==========================================
# STAGE 3: Production Runtime (Alpine)
# ==========================================
FROM alpine:latest
WORKDIR /app

# Install basic libs required by Java on Alpine
RUN apk add --no-cache libstdc++ curl fontconfig ttf-dejavu

# Copy Custom JRE from Backend Stage
ENV JAVA_HOME=/app/java-runtime
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=backend /javaruntime $JAVA_HOME

# Copy Application Layers (Order matters for caching!)
COPY --from=backend /app/dependencies/ ./
COPY --from=backend /app/spring-boot-loader/ ./
COPY --from=backend /app/snapshot-dependencies/ ./
COPY --from=backend /app/application/ ./

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# 4. Generate AppCDS Cache (Critical for Cold Starts)
#    This runs the app briefly during build to create the cache.
#    "|| true" ensures build doesn't fail if DB is unreachable.
RUN java -XX:ArchiveClassesAtExit=application.jsa \
    -Dspring.context.exit=onRefresh \
    -XX:TieredStopAtLevel=1 \
    -Xmx256m \
    org.springframework.boot.loader.launch.JarLauncher || true

USER spring:spring

# Cloud Run Configuration
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
EXPOSE 8080

# FINAL COMMAND: Uses AppCDS + Tiered Compilation
ENTRYPOINT ["java", "-XX:SharedArchiveFile=application.jsa", "-XX:TieredStopAtLevel=1", "-Xmx256m", "-Xss512k", "org.springframework.boot.loader.launch.JarLauncher"]