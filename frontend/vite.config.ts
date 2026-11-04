import { fileURLToPath, URL } from 'node:url'
import path from 'path'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    vueDevTools(),
    createSvgIconsPlugin({
      // 指定缓存的图标文件夹
      iconDirs: [path.resolve(process.cwd(), 'src/assets/icons')],
      // 指定 symbolId 格式
      symbolId: 'icon-[dir]-[name]',
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    // 本地 npm run dev 时把接口转发到本机 Spring Boot，生产环境仍由 server.js 代理。
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/acgn': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/style/variable.scss" as *;`,
      },
    },
  },

  /* ===== 打包配置 ===== */
  build: {
    target: 'esnext',
    outDir: 'dist',
    assetsDir: 'assets',
    assetsInlineLimit: 4096,
    cssCodeSplit: true,
    sourcemap: false,
    minify: 'terser',
    rollupOptions: {
      output: {
        entryFileNames: 'js/[name]-[hash].js',
        chunkFileNames: 'js/[name]-[hash].js',
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
        // 说明：原先用 manualChunks 把所有 node_modules 塞进单个 vendor，结果 mapbox-gl /
        // leaflet / epubjs 这些只在懒加载路由里用到的库也被入口静态 import，首屏必须下完
        // 3.96MB 才出画面。改为交给 Rollup 按实际引用图切分：入口只剩 vue + element-plus，
        // 地图/阅读器跟着对应路由 chunk 走，不进首屏。
      },
    },
  },

  /* ===== 预览生产包 ===== */
  preview: {
    port: 4173,
    open: true,
  },
})