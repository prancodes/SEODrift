# ==========================================
# STAGE 1: Frontend Build (Tailwind/Vite)
# ==========================================
FROM node:20-alpine AS frontend
WORKDIR /app

# Cache npm dependencies first to speed up re-builds
COPY package.json package-lock.json ./
RUN npm ci --silent --prefer-offline

# Copy config and source for build
COPY vite.config.js postcss.config.mjs ./
COPY src/main/resources/static ./src/main/resources/static
COPY src/main/resources/templates ./src/main/resources/templates

# Build Tailwind/CSS (Output: src/main/resources/static/dist)
RUN npm run build

# ==========================================
# STAGE 2: Backend Build & JLink (Java 25)
# ==========================================
FROM eclipse-temurin:25-jdk-alpine AS backend
WORKDIR /app

# 1. Cache Maven wrapper + dependencies before copying source
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -q

# 2. Inject built frontend assets
COPY --from=frontend /app/src/main/resources/static/dist ./src/main/resources/static/dist

# 3. Copy source & build (tests skipped, no devtools in prod jar)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B -q && \
    java -Djarmode=layertools -jar target/*.jar extract

# 4. Create Minimal Java Runtime (JLink)
#    This creates a custom JRE with ONLY the modules Spring Boot needs
RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.logging,java.naming,java.management,java.security.jgss,java.instrument,jdk.unsupported,java.sql,java.net.http,java.xml,jdk.jfr,jdk.crypto.ec,java.desktop,java.compiler,jdk.management,java.xml.crypto,jdk.charsets,jdk.crypto.cryptoki,jdk.jcmd \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=zip-9 \
    --output /javaruntime

# ==========================================
# STAGE 3: Production Runtime (Minimal Alpine)
# ==========================================
FROM alpine:3.21
WORKDIR /app

# libstdc++ is required by the JVM; wget for healthcheck
RUN apk add --no-cache libstdc++ wget

# Create non-root user early and give it ownership of the working directory
RUN addgroup -S spring && adduser -S spring -G spring && chown spring:spring /app

# Copy custom JRE with correct ownership
ENV JAVA_HOME=/app/java-runtime
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --chown=spring:spring --from=backend /javaruntime $JAVA_HOME

# Switch to non-root user BEFORE generating files
USER spring:spring

# Generate base JVM CDS archive (required for custom jlink environments)
RUN java -Xshare:dump

# Copy Application Layers
COPY --chown=spring:spring --from=backend /app/dependencies/ ./
COPY --chown=spring:spring --from=backend /app/spring-boot-loader/ ./
COPY --chown=spring:spring --from=backend /app/snapshot-dependencies/ ./
COPY --chown=spring:spring --from=backend /app/application/ ./

# Perform Class Data Sharing (CDS) training run using the mock build profile.
# No hardcoded secrets here. All database/API classes are successfully cached!
RUN java -XX:ArchiveClassesAtExit=application.jsa \
  -Dspring.profiles.active=cds \
  -Dspring.context.exit=onRefresh \
  org.springframework.boot.loader.launch.JarLauncher

# Cloud Run / GCP configuration
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
EXPOSE 8080

# JVM flags tuned for GCP Cloud Run (fast startup + adaptive heap):
#   -XX:SharedArchiveFile           → uses the pre-compiled class-metadata archive (saves ~30% startup time)
#   -XX:TieredStopAtLevel=1         → JIT level 1 (fast compilation for serverless)
#   -XX:+UseSerialGC                → smallest footprint for single-core containers
#   -XX:MaxRAMPercentage=75.0       → dynamically adapts to whatever GCP assigns
#   -XX:+OptimizeStringConcat       → micro-opt for Thymeleaf rendering
#   -Dspring.jmx.enabled=false      → skip JMX registry (saves ~100ms startup)
#   -Dspring.backgroundpreinitializer.ignore=true → skip pre-init thread
#   -Djava.security.egd=file:/dev/./urandom → use non-blocking entropy on Linux/GCP
#                                            fixes "SecureRandom SHA1PRNG took 247ms"
#                                            (containers have low /dev/random entropy)
ENTRYPOINT ["java", \
  "--enable-native-access=ALL-UNNAMED", \
  "-XX:SharedArchiveFile=application.jsa", \
  "-XX:TieredStopAtLevel=1", \
  "-XX:+UseSerialGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+OptimizeStringConcat", \
  "-Dspring.jmx.enabled=false", \
  "-Dspring.backgroundpreinitializer.ignore=true", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]