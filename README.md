<div align="center">
  <img src="src/main/resources/static/images/navbar-rocket.svg" alt="SEODrift Logo" width="80" height="80">
  
  # SEODrift
  
  > All-in-one YouTube creator intelligence platform — competitor tracking, keyword velocity analysis, AI-powered content workspace, video publishing, and SEO analytics tools.
  
  A modern **Spring Boot** application designed to streamline your YouTube workflow with powerful creator intelligence and optimization tools.

   <a href="https://seodrift-378036956146.us-central1.run.app">
    <img src="https://img.shields.io/badge/🌐%20Live%20Demo-%20Visit-brightblue?style=for-the-badge" alt="Live Demo">
   </a>

  <br/>

  <!-- Badges -->
  <div align="center">
    <img alt="Status" src="https://img.shields.io/badge/status-live-brightgreen?style=for-the-badge&logo=github">
    <img alt="Java" src="https://img.shields.io/badge/Java-25-orange?style=for-the-badge&logo=openjdk">
    <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-4.0.7-brightgreen?style=for-the-badge&logo=springboot">
    <img alt="Tailwind CSS" src="https://img.shields.io/badge/Tailwind%20CSS-4.1.17-blue?style=for-the-badge&logo=tailwindcss">
    <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-Neon.tech-4169E1?style=for-the-badge&logo=postgresql">
    <img alt="Redis" src="https://img.shields.io/badge/Redis-Aiven%20Cloud-DC382D?style=for-the-badge&logo=redis">
    <img alt="GCP" src="https://img.shields.io/badge/GCP-Cloud%20Run-4285F4?style=for-the-badge&logo=googlecloud">
    <img alt="Docker" src="https://img.shields.io/badge/Docker-Multi--Stage-2496ED?style=for-the-badge&logo=docker">
    <img alt="License" src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge&logo=github">
  </div>
</div>

---

## ✨ Features

### 🔐 **Google OAuth2 Authentication & User Sync**
* Secure login via **Google OAuth2** with YouTube API scopes.
* Real-time profile database synchronization to PostgreSQL.
* OAuth2 tokens are **AES-GCM encrypted** before persistence.
* **Redis-backed HTTP sessions** for stateless server scalability.
* **Google OAuth2 Verification Compliant**: Custom `/privacy` and `/terms` routes with explicit API scope disclosures.

### ⚡ **Cache & Edge Gateway Infrastructure**
* **Distributed Caching**: Spring Cache backed by Redis to store analytics and tag queries.
* **Edge Routing Gateway**: Spring Cloud Gateway WebMVC intercepts API requests and securely appends credentials.
* **Sliding-Window Rate Limiting**: Redis filter limits traffic to protect YouTube API quotas.

### 📊 **Creator Console Dashboard**
- **Live Channel Intelligence**: Fetches subscriber count, views, watch time, impressions, and CTR via YouTube Data API and YouTube Analytics API.
- **Geographic Audience Map**: World map of top-10 country view distributions — falls back to the channel's registered country.
- **Historical Growth Charts**: Channel snapshots (taken at most once per 12 hours) plotted as time-series for subscriber, view, and video count growth.
- **Recent Uploads Table**: Lists the 10 latest videos with per-video SEO score, engagement rate, views, and likes.
- **DB Cache Fallback**: Serves last-cached metrics from the `users` table when YouTube API is unavailable.

### 🕵️ **Competitor Intel**
* **Track Competitor Channels** by channel ID or `@handle`.
* **Daily Automated Scraping**: ShedLock-protected cron job (2 AM daily) prevents duplicate Cloud Run scrapes.
* **Posting Rhythm Analysis**: Day-of-week and hour-of-day upload distributions.
* **Topic Momentum Engine**: Ranks keywords from competitor titles to surface trending themes.
* **Subscriber Benchmarking**: Chart.js time-series comparing your channel against competitors.

### 📈 **Keyword Search Velocity**
* **Keyword Tracking**: Monthly video publication count time-series.
* **Growth Rate Engine**: Computes velocity using rolling 30-day YouTube Search API windows.
* **AI-Powered SEO Analysis** (Gemini): Difficulty rating and actionable SEO tips, cached in the database.

### 🤖 **AI Content Workspace** (Gemini)
* **Topic-Driven Generation**: Fetches live competitor context to generate optimized metadata.
* **Full Metadata Package**: Returns titles, descriptions, scripts, tags, and chapter markers.
* **Draft Management**: Save drafts to PostgreSQL and reload them instantly.
* **Publishing Integration**: Draft to publish in a single flow.

### 🏷️ **SEO Tags Generator**
- Extract high-ranking tags from competitor videos or generate optimized ones based on keywords.
- Analyze video metadata for better YouTube visibility.
- Copy tags instantly to your clipboard with structured feedback.
- View related videos with suggested tags for inspiration.

### 🖼️ **Thumbnail Grabber**
- Download high-quality thumbnails from any YouTube video instantly.
- Fallback metadata logic: attempts YouTube Data API v3 first, falling back to oEmbed API if the quota is reached.
- Multiple quality options: MaxRes (1280x720), HD (720x480), SD (480x360), and default.
- Automated download response handler with content-type mapping and filename sanitization.

### 📊 **Video Intelligence Audits**
- **Metric Analytics**: Displays view count, likes, comment count, and dislike count (using the Return YouTube Dislike API).
- **Sentiment Scoring**: Calculates overall audience approval ratio.
- **SEO Health Heuristics**: Analyzes title length, tag presence, title-tag synergy, and calls-to-action (CTAs) in the description.

### 🚀 **Direct YouTube Publishing Hub (Publishing Gatekeeper)**
- **Direct YouTube Upload**: Publish videos directly to YouTube from the SEODrift dashboard.
- **SEO Readiness Scoring**: Blocks upload if title, description, tags, links, and chapters don't meet the 70/100 threshold.
- **Privacy Controls**: Native support for Video Privacy Status (Private, Public, Unlisted).
- **Resumable Uploads**: Chunked uploading via Google API `MediaHttpUploader` with real-time progress tracking.

---

## 🚀 Tech Stack

### Backend
- **Java 25** - JDK 25 AOT Cache (JEP 483/514) for fast serverless cold starts.
- **Spring Boot 4.0.7** - Standardized application framework.
- **Spring Cloud Gateway (WebMVC)** - Handles edge API routing and proxying.
- **Spring Security 7** - OAuth2/OIDC login, AES-GCM token encryption, Redis session management.
- **Spring Data JPA** - Repository layer for PostgreSQL database mapping.
- **Spring WebFlux** - Non-blocking WebClient for YouTube/Analytics API calls.
- **Spring AI (Google GenAI)** - Gemini integration for workspace content generation and keyword analysis.
- **ShedLock** - Distributed scheduler lock backed by PostgreSQL for single-node cron execution.
- **Thymeleaf & Layout Dialect** - Server-side templates utilizing master layouts and fragments.

### Frontend
- **Tailwind CSS v4** - Sleek utility CSS utility with custom dark mode glassmorphism styles.
- **Hotwired Turbo v8** - Fast, SPA-like client-side routing and page transition handler.
- **Vite v7** - Lightning-fast asset compiling and minification.
- **PostCSS** - CSS compiler extension.
- **Phosphor Icons** - For modern and clean design icons.

### Database & DevOps
- **PostgreSQL** - Production-ready storage hosted on Neon.tech.
- **Aiven Redis** - Managed cloud cache, session store, and rate limiter.
- **Flyway** - Database schema version control (V1–V6 migrations).
- **HikariCP** - Highly optimized connection pooling configured for serverless scaling.
- **GCP Cloud Run** - Serverless container hosting with auto-scaling.
- **Docker** - 3-stage containerization: Node (Vite) → Maven + JLink + AOT training → Alpine runtime.
- **JLink** - Custom lean Java Runtime (zip-9 maximum compression, ~40MB).

---

## 📋 Project Structure

```
SEODrift/
├── src/
│   ├── main/
│   │   ├── java/com/seo/project/
│   │   │   ├── SeoDriftApplication.java      # Spring Boot application entry point
│   │   │   ├── config/                       # Security, Cache, Gateway & controller advices
│   │   │   ├── controller/                   # Web endpoints & API controllers (Analytics, Dashboard)
│   │   │   ├── dto/                          # Data Transfer Objects (DTOs)
│   │   │   ├── exception/                    # Global Exception Handler and error responses
│   │   │   ├── model/                        # JPA Database Entities (User, VideoAnalysis, etc.)
│   │   │   ├── repository/                   # Spring Data JPA repositories
│   │   │   └── service/                      # Business logic services
│   │   └── resources/
│   │       ├── application.properties            # Core configurations & credentials mappings
│   │       ├── application-dev.properties        # Profile override for local environment
│   │       ├── application-prod.properties       # Profile override for production deployment
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   ├── base/                     # Core layout styles
│   │       │   │   └── components/               # Module styles (e.g. login modal, navbar, dashboard)
│   │       │   ├── js/
│   │       │   │   ├── core/                 # Theme handler, clipboard helper scripts
│   │       │   │   ├── components/           # Client scripts for widgets and navigation
│   │       │   │   └── main.js               # Core Javascript file orchestrating turbo loads
│   │       │   └── dist/                     # Compressed styles and compiled production builds
│   │       └── templates/
│   │           └── fragments/
│   │               ├── components/           # Thymeleaf reusable components
│   │               └── layout/               # Master wrappers and header/footer navigations
│   │
├── Dockerfile                                # Multistage deployment container configuration
├── package.json                              # Frontend Node package definitions
├── vite.config.js                            # Vite compiler configuration
└── pom.xml                                   # Maven dependency definitions
```

---

## ⚙️ Installation

### Prerequisites
- **Java 25** - [Download JDK 25](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.6+** - [Download Maven](https://maven.apache.org/download.cgi)
- **Node.js 20+ & npm 9+** - [Download Node.js](https://nodejs.org/)
- **PostgreSQL Database** - A local database or a [Neon.tech](https://neon.tech/) cloud instance.
- **YouTube API Key** - Obtain from the [Google Developers Console](https://developers.google.com/youtube/registering_an_application).
- **Google OAuth Client Credentials** - Set up web application credentials in the Google Developers Console (OAuth consent screen).

### Setup Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/prancodes/SEODrift.git
   cd SEODrift
   ```

2. **Create environment configuration**
   Create a `.env` file in the root folder based on `.env.example`:
   ```bash
   cp .env.example .env
   ```
   
   Fill in your credential details:
   ```env
   # Database (Neon.tech / Local Postgres)
   DB_URL=jdbc:postgresql://your-database-host.neon.tech/seodrift?sslmode=require
   DB_USER=your_db_username
   DB_PASSWORD=your_db_password

   # Google OAuth2 Credentials
   GOOGLE_CLIENT_ID=your-google-oauth2-client-id
   GOOGLE_CLIENT_SECRET=your-google-oauth2-client-secret

   # Gemini Configuration (Google AI Studio)
   GEMINI_API_KEY=your_gemini_api_key_here
   GEMINI_MODEL=your_gemini_model_here

   # YouTube API v3 Settings
   YT_API_KEY=your_youtube_api_key_here
   BASE_URL=https://www.googleapis.com/youtube/v3
   APP_URL=http://localhost:8080

   # Redis Configuration (Aiven)
   REDIS_HOST=your-redis-hostname.aivencloud.com
   REDIS_PORT=your-redis-port
   REDIS_PASSWORD=your_redis_password
   REDIS_SSL_ENABLED=true

   # OAuth2 Token Encryption
   OAUTH2_ENCRYPT_KEY=your_16char_secret_key
   OAUTH2_ENCRYPT_SALT=your_16char_hex_salt

   # Spring Configuration
   SPRING_PROFILES_ACTIVE=dev
   PORT=8080
   ```

3. **Install Node dependencies**
   ```bash
   npm install
   ```

4. **Compile frontend assets**
   ```bash
   npm run build
   ```

5. **Build and run the application**
   ```bash
   ./mvnw clean package
   ./mvnw spring-boot:run
   ```

The application will start on `http://localhost:8080` by default.

---

## 🛠️ Development

### Local Dev Workflow

To run with live reloads for style adjustments and server updates, open two terminal windows:

```bash
# Terminal 1: Compile CSS assets in watch mode
npm run dev

# Terminal 2: Run Spring Boot server with DevTools hot swap
./mvnw spring-boot:run
```

- **Thymeleaf Caching**: Disabled in the dev profile (`application-dev.properties`) to reload layouts instantly.
- **Auto Restart**: Enabled via DevTools for modifications within `/src/main/java`.
- **Database Validations**: `ddl-auto` is set to `validate` to ensure safety and data integrity.

---

## 🐳 Docker Deployment

### Run Container Locally with Docker Compose

Ensure your `.env` contains all database, oauth, and api credentials before launching:

```bash
# Build and run the app container
docker compose up --build

# Run in detached (background) mode
docker compose up --build -d

# Shutdown the setup
docker compose down
```

### Advanced Dockerfile Features
- **Multi-Stage Compilation**: Splits Node (Vite/Tailwind) compiling and Maven backend packing into separate stages.
- **Lean JRE (JLink)**: Cuts away unused Java modules and uses `zip-9` maximum compression to output a tailored JVM runtime (~40MB).
- **GCP-Tuned JVM Flags**: `-XX:AOTCache=app.aot` (JDK 25 AOT cache), `-XX:+UseG1GC`, `-XX:MaxRAMPercentage=75.0`, and `-Djava.security.egd=file:/dev/./urandom` for fast cold-start on Cloud Run.
- **Non-Root Execution**: Runs under user `spring` to limit host vulnerabilities.
- **Actuator Health Checks**: Docker health status via `wget` on `/actuator/health`.

---

## 📦 Core Dependencies

### Backend
| Group ID / Artifact ID | Version | Description |
|------------------------|---------|-------------|
| `org.springframework.boot:spring-boot-starter-webmvc` | `4.0.7` | REST endpoints & Web MVC support |
| `org.springframework.boot:spring-boot-starter-webflux` | `4.0.7` | Non-blocking HTTP WebClient |
| `org.springframework.boot:spring-boot-starter-security` | `4.0.7` | Security infrastructure |
| `org.springframework.boot:spring-boot-starter-oauth2-client` | `4.0.7` | Google OAuth2 and OpenID Connect client |
| `org.springframework.boot:spring-boot-starter-data-jpa` | `4.0.7` | ORM database connection |
| `org.springframework.boot:spring-boot-starter-data-redis` | `4.0.7` | Lettuce Redis client |
| `org.springframework.boot:spring-boot-starter-session-data-redis` | `4.0.7` | Redis-backed HTTP sessions |
| `org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc` | `2025.1.0` | API proxy & Redis rate limiter |
| `org.springframework.ai:spring-ai-starter-model-google-genai` | `1.1.7` | Gemini AI integration |
| `net.javacrumbs.shedlock:shedlock-spring` | `5.16.0` | Distributed scheduler locking |
| `org.postgresql:postgresql` | `Runtime` | PostgreSQL JDBC driver |
| `com.google.apis:google-api-services-youtube` | `v3-rev20260602-2.0.0` | YouTube Data API v3 & Resumable MediaHttpUploader |

### Frontend
| Package Name | Version | Description |
|--------------|---------|-------------|
| `tailwindcss` | `^4.1.17` | Utility layout system |
| `@hotwired/turbo` | `^8.0.23` | High-speed SPA page routing |
| `vite` | `^7.2.6` | Hot-reloading asset builder |
| `vite-plugin-compression2` | `^2.5.3` | Production compression (Brotli & Gzip) |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Pranjal Singh** - [@prancodes](https://github.com/prancodes)

---

## ⭐ Show Your Support

If you find this project helpful, please consider giving it a star on GitHub!

