// vite.config.js
import { defineConfig } from 'vite';
import path from 'path';

export default defineConfig({
  build: {
    // Output compiled files to Spring Boot's static directory
    outDir: 'src/main/resources/static/dist',
    emptyOutDir: true, // Clean the folder before building
    rollupOptions: {
      input: {
        // Point to your source CSS file
        main: path.resolve(__dirname, 'src/main/resources/static/css/input.css'),
      },
      output: {
        // Force a constant file name (avoid hashing like main-x82z.css) for easier Thymeleaf linking
        assetFileNames: 'styles.css',
      }
    }
  }
});