import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import path from 'path'

export default defineConfig({
  base: '/', 
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'logo192.png', 'logo512.png'],
      manifest: {
        name: 'Haru - 당신의 취미를 공유하세요',
        short_name: 'Haru',
        description: 'Haru - 당신의 취미를 공유하세요',
        theme_color: '#000000',
        background_color: '#ffffff',
        start_url: '/',
        display: 'standalone',
        screenshots: [
          {
            src: "/screenshot1.png",
            sizes: "540x720",
            type: "image/png",
            form_factor: "narrow"
          },
          {
            src: "/screenshot2.png",
            sizes: "720x540",
            type: "image/png",
            form_factor: "wide"
          }
        ],
        icons: [
          {
            src: '/apple-touch-icon.png',
            sizes: '180x180',
            type: 'image/png'
          },
          {
            src: '/favicon-16x16.png',
            sizes: '16x16',
            type: 'image/png'
          },
          {
            src: '/favicon-32x32.png',
            sizes: '32x32',
            type: 'image/png'
          },
          {
            src: '/favicon-64x64.png',
            sizes: '64x64',
            type: 'image/png'
          },
          {
            src: '/favicon-96x96.png',
            sizes: '96x96',
            type: 'image/png'
          },
          {
            src: '/favicon.ico',
            sizes: '64x64 32x32 24x24 16x16',
            type: 'image/x-icon'
          },
          {
            src: '/favicon.svg',
            sizes: 'any',
            type: 'image/svg+xml'
          },
          {
            src: '/logo192.png',
            sizes: '192x192',
            type: 'image/png'
          },
          {
            src: '/logo512.png',
            sizes: '512x512',
            type: 'image/png'
          }
        ]
      },
      workbox: {
        maximumFileSizeToCacheInBytes: 10 * 1024 * 1024, // 10MB로 증가
        globPatterns: ['**/*.{js,css,html,ico,png,svg,jpg}'],
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/api\.yourbackend\.com\/.*/i,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              expiration: {
                maxEntries: 10,
                maxAgeSeconds: 60 * 60 * 24 // 24시간
              }
            }
          }
        ]
      }
    })
  ],
  define: {
    global: 'globalThis'
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      crypto: 'crypto-browserify',
      stream: 'stream-browserify',
      buffer: 'buffer',
      util: 'util',
      path: 'path-browserify'
    },
  },
  optimizeDeps: {
    esbuildOptions: {
      define: {
        global: 'globalThis'
      }
    }
  },
  server: {   
    host: true,
    port: 3000,
    allowedHosts: [
      'sunbee.world',
      'www.sunbee.world',
      'localhost',
    ],
  },
  build: {
    rollupOptions: {
      input: {
        main: '/index.html',
      },      
      output: {
        manualChunks: {
          'opencv': ['opencv.js']
        }
      }
    },
  },
})