# 🎬 SEODrift

> All-in-one toolkit to optimize tags, download thumbnails, and save videos for your YouTube content creation workflow.

A modern **Spring Boot** application designed to streamline YouTube content creators' workflow with powerful SEO and video management tools.

---

## ✨ Features

### 🏷️ **SEO Tags Generator**
- Extract high-ranking tags from competitor videos or generate new ones based on keywords
- Optimize your video metadata for better YouTube visibility
- Analyze and suggest tags to boost your video's searchability

### 🖼️ **Thumbnail Grabber**
- Download high-quality thumbnails from any YouTube video instantly
- Multiple quality options: HD, SD, and MaxRes resolution
- Perfect for inspiration, analysis, or content research

### 📥 **Video Downloader**
- Download YouTube videos in multiple formats and quality options
- Support for various resolutions and audio/video combinations
- Streamline your content archival and backup workflow

---

## 🚀 Tech Stack

### Backend
- **Java 25** - Latest Java version for modern language features
- **Spring Boot 4.0.0** - Production-ready framework
- **Spring MVC** - Web framework for request handling
- **Spring WebFlux** - Reactive programming support
- **Thymeleaf** - Server-side template engine
- **Jackson** - JSON data binding and processing

### Frontend
- **Tailwind CSS 4.1.17** - Utility-first CSS framework
- **PostCSS 8.5.6** - CSS transformations
- **Vite 7.2.6** - Lightning-fast build tool

### Build Tools
- **Maven** - Dependency management and project build
- **Lombok** - Reduce boilerplate code
- **Spring DevTools** - Hot reload during development

---

## 📋 Project Structure

```
SEODrift/
├── src/
│   ├── main/
│   │   ├── java/com/seo/project/
│   │   │   ├── SeoDriftApplication.java      # Spring Boot entry point
│   │   │   └── Controller/
│   │   │       └── WebController.java        # Web request handlers
│   │   └── resources/
│   │       ├── application.properties        # Application configuration
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   └── input.css            # Tailwind CSS input
│   │       │   ├── js/                      # JavaScript assets
│   │       │   └── dist/                    # Compiled CSS (generated)
│   │       └── templates/
│   │           ├── index.html               # Dashboard landing page
│   │           ├── tags.html                # SEO tag generator page
│   │           ├── thumbnail.html           # Thumbnail grabber page
│   │           ├── video.html               # Video downloader page
│   │           └── fragments/
│   │               ├── layout.html          # Base layout template
│   │               ├── navbar.html          # Navigation component
│   │               └── footer.html          # Footer component
│   └── test/
│       └── java/com/seo/project/
│           └── SeoDriftApplicationTests.java
├── pom.xml                                   # Maven configuration
├── package.json                              # NPM configuration
├── vite.config.js                           # Vite build configuration
├── postcss.config.mjs                       # PostCSS configuration
└── README.md                                # Project documentation
```

---

## ⚙️ Installation

### Prerequisites
- **Java 25**
- **Maven 3.6+**
- **Node.js 18+** (for frontend assets)
- **npm 9+**

### Setup Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/prancodes/SEODrift.git
   cd SEODrift
   ```

2. **Install Node dependencies** (for frontend build tools)
   ```bash
   npm install
   ```

3. **Build the project with Maven**
   ```bash
   ./mvnw clean package
   ```

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

The application will start on `http://localhost:8080` by default.

---

## 🛠️ Development

### Building CSS Assets
The project uses Tailwind CSS with Vite for fast CSS compilation:

```bash
# Watch mode - Automatically rebuild CSS on changes
npm run dev

# Production build
npm run build
```

This compiles `src/main/resources/static/css/input.css` to `src/main/resources/static/dist/styles.css`.

### Hot Reload
Spring DevTools is included for automatic restart during development:
- Modify Java files → Automatic recompilation
- Modify templates → Auto-refresh (use browser live reload)
- Modify CSS → Rebuild with `npm run dev`

### Running Tests
```bash
./mvnw test
```

---

## 🌐 Usage

### Navigate to the Dashboard
Open your browser and visit `http://localhost:8080` to see the main dashboard with three feature cards:

1. **SEO Tags Generator** (`/tags`)
   - Enter a YouTube video URL or keywords
   - Get optimized tag suggestions
   - Copy tags for use in your video metadata

2. **Thumbnail Grabber** (`/thumbnail`)
   - Paste a YouTube video URL
   - Download high-quality thumbnails
   - Choose from HD, SD, or MaxRes options

3. **Video Downloader** (`/video`)
   - Paste a YouTube video URL
   - Select your preferred format and quality
   - Download video files for offline access

---

## 🏗️ Build Configuration

### Maven (`pom.xml`)
- **Group ID**: `com.seo`
- **Artifact ID**: `SEODrift`
- **Version**: `0.0.1-SNAPSHOT`
- **Parent**: Spring Boot Starter Parent 4.0.0

### Vite (`vite.config.js`)
- Input: `src/main/resources/static/css/input.css`
- Output: `src/main/resources/static/dist/styles.css`
- Auto-cleanup of output directory

---

## 📦 Dependencies

### Key Backend Dependencies
- `spring-boot-starter-thymeleaf` - Template rendering
- `spring-boot-starter-webflux` - Reactive web framework
- `spring-boot-starter-webmvc` - Traditional web framework
- `jackson-databind` - JSON processing
- `spring-boot-devtools` - Development tooling
- `projectlombok` - Boilerplate reduction

### Key Frontend Dependencies
- `tailwindcss@4.1.17` - CSS framework
- `vite@7.2.6` - Build tool
- `postcss@8.5.6` - CSS processing

---

## 🔗 Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Tailwind CSS Documentation](https://tailwindcss.com)
- [Vite Documentation](https://vitejs.dev)
- [Thymeleaf Documentation](https://www.thymeleaf.org)

---

## 👨‍💻 Author

**Pranjal Singh** - [@prancodes](https://github.com/prancodes)

