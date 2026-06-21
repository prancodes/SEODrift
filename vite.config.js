// vite.config.js
import { defineConfig } from 'vite';
import path from 'path';
import { compression } from 'vite-plugin-compression2';

export default defineConfig({
  base: '/dist/',
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
        workspace: path.resolve(__dirname, 'src/main/resources/static/js/components/workspace-bundle.js'),
        dashboard: path.resolve(__dirname, 'src/main/resources/static/js/components/dashboard-console.js'),
        trends: path.resolve(__dirname, 'src/main/resources/static/js/components/trends-console.js'),
        keywords: path.resolve(__dirname, 'src/main/resources/static/js/components/keywords-console.js'),
      },
      output: {
        entryFileNames: '[name].js',
        assetFileNames: (assetInfo) => {
          if (assetInfo.name.endsWith('.css')) return '[name].css';
          return '[name][extname]';
        }
      }
    }
  }
});