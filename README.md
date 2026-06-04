<div align="center">
  <img src="src/main/resources/static/images/navbar-rocket.svg" alt="SEODrift Logo" width="80" height="80">
  
  # SEODrift
  
  > All-in-one toolkit for SEO optimization: generate tags, grab thumbnails, and analyze video performance metrics.
  
  A modern **Spring Boot** application designed to streamline your workflow with powerful SEO optimization and analytics tools.

   <a href="https://seodrift-378036956146.us-central1.run.app">
    <img src="https://img.shields.io/badge/🌐%20Live%20Demo-%20Visit-brightblue?style=for-the-badge" alt="Live Demo">
   </a>

  <br/>

  <!-- Badges -->
  <div align="center">
    <img alt="Status" src="https://img.shields.io/badge/status-live-brightgreen?style=for-the-badge&logo=github">
    <img alt="Java" src="https://img.shields.io/badge/Java-25-orange?style=for-the-badge&logo=openjdk">
    <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?style=for-the-badge&logo=springboot">
    <img alt="Tailwind CSS" src="https://img.shields.io/badge/Tailwind%20CSS-4.1.17-blue?style=for-the-badge&logo=tailwindcss">
    <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-Neon.tech-4169E1?style=for-the-badge&logo=postgresql">
    <img alt="Redis" src="https://img.shields.io/badge/Redis-Aiven%20Cloud-DC382D?style=for-the-badge&logo=redis">
    <img alt="Docker" src="https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker">
    <img alt="License" src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge&logo=github">
  </div>
</div>

---

## ✨ Features

### 🔐 **Google OAuth2 Authentication & User Sync**
- Dual login paths: **Google One Tap** (top-right card on page load) and full **OAuth2 Authorization Code** flow via the Login button.
- Automated client-side JWT processing and server-side token verification via `GoogleIdTokenVerifier`.
- Real-time profile database synchronization (names, emails, Google IDs, and profile images).
- Dynamic navigation state changes based on authentication context.
- **Google OAuth2 Verification Compliant**: Integrated custom `/privacy` and `/terms` routes with explicit API scopes disclosure (including YouTube Data API), data revocation, and contact email support (`prancoder@gmail.com`) to pass manual OAuth2 reviews.

### ⚡ **Cache & Edge Gateway Infrastructure**
- **Distributed Caching**: Configured Spring Cache backed by Redis (with Lettuce connection pools) to store analytics and tag queries, minimizing external API calls.
- **Edge Routing Gateway**: Intercepts downstream YouTube search/details requests via Spring Cloud Gateway WebMVC, appending credentials securely.
- **Sliding-Window Rate Limiting**: Employs a custom Redis-based sliding-window filter on API gateways to throttle traffic and protect YouTube API quotas.

### 📊 **Personalized User Dashboard & History**
- **Search History Tracking**: Automatically persists SEO audits to PostgreSQL (Neon.tech) for logged-in users.
- **Aggregated Analytics**: Displays user metrics including total audits performed, average SEO health scores, and overall audience engagement rates.
- **Smart Channel Evaluation**: Evaluates channel health status based on historical audits (Excellent, Needs Work, Critical).
- **Asynchronous Data Feed**: History items are lazy-loaded via Thymeleaf fragments and Turbo frames.

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

---

## 🚀 Tech Stack

### Backend
- **Java 25** - Utilizing modern language features and optimized runtimes.
- **Spring Boot 4.0.6** - Standardized application framework.
- **Spring Cloud Gateway (WebMVC)** - Handles edge API routing and proxying.
- **Spring Security 7** - Robust security posture handling session management and OAuth2/OIDC.
- **Spring Data JPA** - Repository layer for PostgreSQL database mapping.
- **Spring WebFlux** - Non-blocking WebClient for high-performance external API calls.
- **Thymeleaf & Layout Dialect** - Server-side templates utilizing master layouts and fragments.

### Frontend
- **Tailwind CSS v4** - Sleek utility CSS utility with custom dark mode glassmorphism styles.
- **Hotwired Turbo v8** - Fast, SPA-like client-side routing and page transition handler.
- **Vite v7** - Lightning-fast asset compiling and minification.
- **PostCSS** - CSS compiler extension.
- **Phosphor Icons** - For modern and clean design icons.

### Database & DevOps
- **PostgreSQL** - Production-ready storage hosted on Neon.tech.
- **Aiven Redis** - Managed cloud cache and rate limiting database.
- **Flyway** - Database schema version control.
- **HikariCP** - Highly optimized connection pooling configured for serverless scaling.
- **Docker** - Multi-stage containerization compiling assets and packing the app.
- **JLink** - Custom lean Java Runtime (using zip-9 maximum compression) resulting in minimal image footprint.

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

   # YouTube API v3 Settings
   YT_API_KEY=your_youtube_api_key_here
   BASE_URL=https://www.googleapis.com/youtube/v3
   APP_URL=http://localhost:8080

   # Redis Configuration (Aiven)
   REDIS_HOST=your-redis-hostname.aivencloud.com
   REDIS_PORT=your-redis-port
   REDIS_PASSWORD=your_redis_password
   REDIS_SSL_ENABLED=true

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
- **GCP-Tuned JVM Flags**: `-XX:+UseSerialGC`, `-XX:MaxRAMPercentage=75.0`, and `-Djava.security.egd=file:/dev/./urandom` for fast cold-start on Cloud Run.
- **Non-Root Execution**: Runs under user `spring` to limit host vulnerabilities.
- **Actuator Health Checks**: Docker health status via `wget` on `/actuator/health`.

---

## 📦 Core Dependencies

### Backend
| Group ID / Artifact ID | Version | Description |
|------------------------|---------|-------------|
| `org.springframework.boot:spring-boot-starter-web` | `4.0.6` | REST endpoints & Web MVC support |
| `org.springframework.boot:spring-boot-starter-webflux` | `4.0.6` | Non-blocking HTTP WebClient |
| `org.springframework.boot:spring-boot-starter-security` | `4.0.6` | Security infrastructure |
| `org.springframework.boot:spring-boot-starter-oauth2-client`| `4.0.6` | Google OAuth2 and OpenID Connect client |
| `org.springframework.boot:spring-boot-starter-data-jpa` | `4.0.6` | ORM database connection |
| `org.postgresql:postgresql` | `Runtime` | PostgreSQL JDBC driver |
| `com.google.api-client:google-api-client` | `2.7.0` | Google Identity Services JWT verifiers |

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
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Pranjal Singh** - [@prancodes](https://github.com/prancodes)

---

## ⭐ Show Your Support

If you find this project helpful, please consider giving it a star on GitHub!

