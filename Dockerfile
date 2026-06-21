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

# 3. Build JAR, rename to app.jar, extract layers via jarmode=tools (Spring Boot 4.x standard)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B -q && \
  mv target/*.jar target/app.jar && \
  java -Djarmode=tools -jar target/app.jar extract --destination application

# 4. Create Minimal Java Runtime (JLink)
#    Custom JRE with ONLY the modules Spring Boot needs
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

# libstdc++ required by JVM; wget for healthcheck
RUN apk add --no-cache libstdc++ wget

# Create non-root user and assign ownership
RUN addgroup -S spring && adduser -S spring -G spring && chown spring:spring /app

# Copy custom JRE
ENV JAVA_HOME=/app/java-runtime
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --chown=spring:spring --from=backend /javaruntime $JAVA_HOME

# Switch to non-root user BEFORE generating cache files
USER spring:spring

# Copy extracted application layers (from jarmode=tools extract)
# AOT Cache requires the extracted layout, NOT the fat jar directly
COPY --chown=spring:spring --from=backend /app/application/ ./

# ─────────────────────────────────────────────────────────────────────────────
# JDK 25 AOT Cache Training Run  (Project Leyden — JEP 483 + JEP 514 + JEP 515)
#
# Upgrade from AppCDS (-XX:ArchiveClassesAtExit):
#   AppCDS    → archives parsed class metadata only        → ~25-30% faster startup
#   AOT Cache → archives classes fully LOADED + LINKED +
#               JIT compiler method profiles               → ~40-50% faster startup
#                                                            + faster JIT warm-up
#
# -XX:AOTCacheOutput=app.aot
#   JDK 25 single-command cache creation (JEP 514).
#   Runs the training execution AND assembles the .aot file in one step.
#   (In JDK 24, this required two separate -XX:AOTMode=record / create steps)
#
# -Dspring.context.exit=onRefresh
#   Tells Spring Boot to exit cleanly immediately after the ApplicationContext
#   has refreshed (all beans initialized). This ensures the cache captures the
#   fully-initialized startup state without needing real DB/Redis connections.
#
# -Dspring.profiles.active=aot-training
#   Activates application-aot-training.properties which stubs out DB, Redis,
#   and external APIs so the training run completes without real credentials.
# ─────────────────────────────────────────────────────────────────────────────
RUN java --enable-native-access=ALL-UNNAMED \
  -XX:AOTCacheOutput=app.aot \
  -Dspring.context.exit=onRefresh \
  -Dspring.profiles.active=aot-training \
  -jar app.jar

# Cloud Run / GCP configuration
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
EXPOSE 8080

# ─────────────────────────────────────────────────────────────────────────────
# Production JVM — tuned for GCP Cloud Run (2 vCPU / 1GiB RAM)
#
#   -XX:AOTCache=app.aot
#     Loads the AOT cache generated above. The JVM skips class loading,
#     linking, and bytecode verification entirely. Also restores JIT method
#     profiles (JEP 515) so the C2 compiler knows which methods are hot
#     immediately, eliminating the usual JIT warm-up penalty.
#
#   -XX:+UseG1GC
#     G1 Garbage Collector — best balance of throughput and pause latency
#     for a web application on 1GB RAM.
#
#   -XX:MaxRAMPercentage=75.0
#     Heap ceiling = ~768MB (75% of 1GB container RAM).
#
#   -Xms64m
#     Initial heap = 64MB. Prevents frequent heap resizing during cold start.
#
#   -XX:+OptimizeStringConcat
#     Optimizes StringBuilder usage. Meaningful benefit for Thymeleaf rendering.
#
#   -Dspring.jmx.enabled=false
#     Disables JMX MBean registry. Saves ~100ms on startup.
#
#   -Dspring.backgroundpreinitializer.ignore=true
#     Skips Spring's background pre-initialization thread (not needed for prod).
#
#   -Djava.security.egd=file:/dev/./urandom
#     Non-blocking entropy source. Prevents SecureRandom stalls during SSL init.
#
# NOTE: -XX:TieredStopAtLevel=1 has been intentionally REMOVED.
#   That flag forces the JVM to only use the C1 (client) compiler and never
#   reach the optimizing C2 compiler. With AOT Cache (JEP 515), the JIT
#   method profiles are pre-loaded, so C2 warms up near-instantly anyway.
#   Keeping TieredStopAtLevel=1 would throw away those profiles and result
#   in permanently degraded peak throughput.
# ─────────────────────────────────────────────────────────────────────────────
ENTRYPOINT ["java", \
  "--enable-native-access=ALL-UNNAMED", \
  "-XX:AOTCache=app.aot", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Xms64m", \
  "-XX:+OptimizeStringConcat", \
  "-Dspring.jmx.enabled=false", \
  "-Dspring.backgroundpreinitializer.ignore=true", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]