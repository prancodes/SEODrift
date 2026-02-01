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
    <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen?style=for-the-badge&logo=springboot">
    <img alt="Tailwind CSS" src="https://img.shields.io/badge/Tailwind%20CSS-4.1.17-blue?style=for-the-badge&logo=tailwindcss">
    <img alt="Docker" src="https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker">
    <img alt="License" src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge&logo=github">
  </div>
</div>

---

## ✨ Features

### 🏷️ **SEO Tags Generator**
- Extract high-ranking tags from competitor videos or generate optimized ones based on keywords
- Analyze video metadata for better YouTube visibility
- Copy tags instantly for use in your video metadata
- View related videos with suggested tags for inspiration

### 🖼️ **Thumbnail Grabber**
- Download high-quality thumbnails from any YouTube video instantly
- Multiple quality options: MaxRes (1280x720), HD (720x480), SD (480x360), and more
- Perfect for inspiration, analysis, or content research
- Direct download with proper filename handling

### 📊 **Video Intelligence Dashboard**
- Deep-dive analytics: view counts, likes, comments, and hidden dislike counts
- Sentiment analysis showing audience approval percentage
- Comprehensive SEO health audit with actionable recommendations
- Engagement rate calculations
- Title optimization analysis
- Tag presence and keyword synergy detection
- Description quality review

---

## 🚀 Tech Stack

### Backend
- **Java 25** - Latest Java version for modern language features
- **Spring Boot 4.0.2** - Production-ready framework
- **Spring MVC** - Traditional web framework for request handling
- **Spring WebFlux** - Reactive programming support for non-blocking I/O
- **Thymeleaf** - Server-side template engine with fragment support
- **Jackson 3.0.0** - JSON data binding and processing

### Frontend
- **Tailwind CSS 4.1.17** - Utility-first CSS framework with dark mode
- **PostCSS 8.5.6** - CSS transformations and processing
- **Vite 7.2.6** - Lightning-fast build tool for frontend assets
- **Phosphor Icons** - Beautiful icon library

### Build & DevOps
- **Maven 3.9.11** - Dependency management and project build
- **Lombok** - Reduce boilerplate code
- **Spring DevTools** - Hot reload during development
- **Docker** - Multi-stage containerization for production
- **Docker Compose** - Local development environment

---

## 📋 Project Structure

```
SEODrift/
├── src/
│   ├── main/
│   │   ├── java/com/seo/project/
│   │   │   ├── SeoDriftApplication.java          # Spring Boot entry point
│   │   │   ├── controller/
│   │   │   │   ├── WebController.java            # Home page handler
│   │   │   │   ├── TagsController.java           # Tags generator
│   │   │   │   ├── ThumbnailController.java      # Thumbnail downloader
│   │   │   │   └── AnalyticsController.java      # Video analytics
│   │   │   ├── service/
│   │   │   │   ├── TagsService.java              # Tags generation logic
│   │   │   │   ├── ThumbnailService.java         # Thumbnail extraction
│   │   │   │   ├── AnalyticsService.java         # Video analytics
│   │   │   │   └── ApiService.java               # API configuration
│   │   │   └── dto/
│   │   │       ├── VideoTagsInfo.java            # Tags DTO
│   │   │       ├── VideoAnalytics.java           # Analytics DTO
│   │   │       ├── ThumbnailRequest.java         # Request model
│   │   │       ├── ThumbnailOptions.java         # Thumbnail options
│   │   │       └── TagsGeneratorResponse.java    # Response model
│   │   └── resources/
│   │       ├── application.properties            # Main configuration
│   │       ├── application-dev.properties        # Development profile
│   │       ├── application-prod.properties       # Production profile
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   └── input.css                # Tailwind CSS input
│   │       │   ├── js/
│   │       │   │   ├── theme.js                 # Dark/light mode toggle
│   │       │   │   └── copy.js                  # Copy to clipboard utility
│   │       │   └── dist/
│   │       │       └── styles.css               # Compiled CSS (generated)
│   │       └── templates/
│   │           ├── index.html                   # Dashboard landing page
│   │           ├── tags.html                    # SEO tags generator
│   │           ├── thumbnail.html               # Thumbnail grabber
│   │           ├── analytics.html               # Video analytics dashboard
│   │           └── fragments/
│   │               ├── layout.html              # Base layout template
│   │               ├── navbar.html              # Navigation component
│   │               └── footer.html              # Footer component
│   └── test/
│       └── java/com/seo/project/
│           └── SeoDriftApplicationTests.java
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── pom.xml                                       # Maven configuration
├── package.json                                  # NPM dependencies
├── vite.config.js                               # Vite build configuration
├── postcss.config.mjs                           # PostCSS configuration
├── Dockerfile                                    # Multi-stage Docker build
├── docker-compose.yml                           # Development environment
├── .gitignore                                   # Git ignore rules
├── .dockerignore                                # Docker ignore rules
└── README.md                                    # Project documentation
```

---

## ⚙️ Installation

### Prerequisites
- **Java 25** - [Download](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.6+** - [Download](https://maven.apache.org/download.cgi)
- **Node.js 20+** - [Download](https://nodejs.org/)
- **npm 9+** - (comes with Node.js)
- **YouTube API Key** - [Get one](https://developers.google.com/youtube/registering_an_application)

### Setup Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/prancodes/SEODrift.git
   cd SEODrift
   ```

2. **Create environment file**
   ```bash
   # Copy the example and fill in your API key
   cp .env.example .env
   ```
   
   Update `.env` with your YouTube API credentials:
   ```env
   YT_API_KEY=your_youtube_api_key_here
   BASE_URL=https://www.googleapis.com/youtube/v3
   ```

3. **Install Node dependencies** (for frontend build tools)
   ```bash
   npm install
   ```

4. **Build frontend assets**
   ```bash
   npm run build
   ```

5. **Build and run with Maven**
   ```bash
   ./mvnw clean package
   ./mvnw spring-boot:run
   ```

The application will start on `http://localhost:8080` by default.

---

## 🛠️ Development

### Local Development Setup

1. **Install dependencies**
   ```bash
   npm install
   ./mvnw dependency:go-offline
   ```

2. **Run in development mode**
   ```bash
   # Terminal 1: Watch CSS changes
   npm run dev
   
   # Terminal 2: Run Spring Boot with hot reload
   ./mvnw spring-boot:run
   ```

### Building CSS Assets
The project uses Tailwind CSS v4 with Vite for fast compilation:

```bash
# Watch mode - Automatically rebuild CSS on file changes
npm run dev

# Production build with optimization
npm run build
```

This processes `src/main/resources/static/css/input.css` and outputs to `src/main/resources/static/dist/styles.css`.

### Features in Development
- **Spring DevTools** - Automatic server restart on Java changes
- **Hot CSS Reload** - CSS changes reflect immediately via Vite
- **Template Caching Disabled** - Thymeleaf templates reload on change
- **Debug Logging** - Enhanced logging for troubleshooting

### Running Tests
```bash
./mvnw test
```

---

## 🐳 Docker Deployment

### Build and Run with Docker Compose

```bash
# Set environment variables
export YT_API_KEY="your_api_key"
export BASE_URL="https://www.googleapis.com/youtube/v3"

# Build and start the container
docker-compose up --build

# Access the app
open http://localhost:8080
```

### Dockerfile Details
- **Stage 1**: Frontend build with Node.js and Vite
- **Stage 2**: Backend build with Maven
- **Stage 3**: Production runtime with optimized JRE
- **Features**:
  - Multi-stage build for minimal image size
  - Non-root user for security
  - Health check included
  - JVM heap limits (300MB max)

### Environment Variables
```env
YT_API_KEY=your_youtube_api_key          # Required: YouTube Data API key
BASE_URL=https://www.googleapis.com/youtube/v3  # YouTube API endpoint
PORT=8080                                 # Server port
SPRING_PROFILES_ACTIVE=prod              # Active profile (dev/prod)
```

---

## 📦 Dependencies

### Backend Dependencies
| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot Starter Web | 4.0.2 | Web framework |
| Spring Boot Starter WebFlux | 4.0.2 | Reactive programming |
| Spring Boot Starter Thymeleaf | 4.0.2 | Template engine |
| Jackson Databind | 3.0.0 | JSON processing |
| Spring Boot DevTools | 4.0.2 | Development tools |
| Lombok | Latest | Boilerplate reduction |

### Frontend Dependencies
| Package | Version | Purpose |
|---------|---------|---------|
| Tailwind CSS | 4.1.17 | Utility CSS framework |
| Vite | 7.2.6 | Build tool |
| PostCSS | 8.5.6 | CSS transformations |
| @tailwindcss/postcss | 4.1.17 | Tailwind PostCSS plugin |

---

## 🤝 Contributing

Contributions are welcome! Here's how:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📚 Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Tailwind CSS Documentation](https://tailwindcss.com/docs)
- [Vite Documentation](https://vitejs.dev/guide/)
- [Thymeleaf Documentation](https://www.thymeleaf.org/)
- [YouTube Data API](https://developers.google.com/youtube/v3)

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 👨‍💻 Author

**Pranjal Singh** - [@prancodes](https://github.com/prancodes)

---

## ⭐ Show Your Support

If you find this project helpful, please consider giving it a star on GitHub!

