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
        manualChunks(id) {
          // 说明：曾尝试按库拆 chunk（vue / element-plus / mapbox / live2d），
          // 但 mapbox-gl 与 vendor 之间有 commonjs helper 的环形依赖，
          // 拆出来之后 mapbox 仍会被首页静态预加载，反而多出一个请求，故保持单 vendor。
          // 真正要优化的话，应把 WarFlag 里的 mapbox 改成动态 import。
          if (id.includes('node_modules')) return 'vendor'
        }
      },
    },
  },

  /* ===== 预览生产包 ===== */
  preview: {
    port: 4173,
    open: true,
  },
})