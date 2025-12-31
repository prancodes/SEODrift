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
# STAGE 3: Get Deno (Critical Fix)
# ==========================================
FROM denoland/deno:bin AS deno

# ==========================================
# STAGE 4: Production Runtime
# ==========================================
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# 1. Install System Deps
#    We need 'xz-utils' to extract the ffmpeg tarball
RUN apt-get update && \
    apt-get install -y python3 python3-pip curl ca-certificates xz-utils && \
    rm -rf /var/lib/apt/lists/*

# 2. ✅ Install Latest FFmpeg (Static Binary Method)
#    Downloads the latest release (v7.x+) which is better for yt-dlp
RUN curl -L https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz -o ffmpeg.tar.xz && \
    tar -xf ffmpeg.tar.xz && \
    # Move binaries to global path
    mv ffmpeg-*-amd64-static/ffmpeg /usr/local/bin/ffmpeg && \
    mv ffmpeg-*-amd64-static/ffprobe /usr/local/bin/ffprobe && \
    # Cleanup
    rm -rf ffmpeg.tar.xz ffmpeg-*-amd64-static

# 3. Install Latest yt-dlp
RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp && \
    yt-dlp --version  # Verify installation

# 4. ✅ INSTALL DENO (Required for BotGuard)
COPY --from=deno /deno /usr/local/bin/deno

# 5. Create app directory and downloads folder
RUN mkdir -p /app/downloads && \
    groupadd -r spring && useradd -r -g spring spring && \
    chown -R spring:spring /app

# 6. Copy Application
COPY --from=backend --chown=spring:spring /app/target/*.jar app.jar
USER spring:spring

# 7. Set environment - Production Profile
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]