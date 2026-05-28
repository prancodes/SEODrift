// vite.config.js
import { defineConfig } from 'vite';
import path from 'path';
import { compression } from 'vite-plugin-compression2';

export default defineConfig({
  plugins: [
    compression(), // Gzip (default)
    compression({ algorithm: 'brotliCompress', exclude: [/\.(br)$/, /\.(gz)$/] }), // Brotli
  ],
  build: {
    // Output compiled files to Spring Boot's static directory
    outDir: 'src/main/resources/static/dist',
    emptyOutDir: true, // Clean the folder before building
    minify: 'esbuild',
    cssCodeSplit: true,
    reportCompressedSize: true,
    rollupOptions: {
      input: {
        main: path.resolve(__dirname, 'src/main/resources/static/js/main.js'),
        styles: path.resolve(__dirname, 'src/main/resources/static/css/base/input.css'),
      },
      output: {
        entryFileNames: '[name].js',
        assetFileNames: (assetInfo) => {
          if (assetInfo.name.endsWith('.css')) return 'styles.css';
          return '[name][extname]';
        }
      }
    }
  }
});