import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],


  define: {
      // process 전체와 process.env를 명시적으로 객체 정의
      'process.env': JSON.stringify({}),
      'process.browser': true,
      'process': { env: {} },
      global: 'window',
    },

  build: {
    outDir: path.resolve(import.meta.dirname, '../src/main/resources/static/js/react'),
    emptyOutDir: true,
    // 라이브러리(부품) 모드로 설정하여 index.html 생성을 막습니다.
    lib: {
      entry: path.resolve(import.meta.dirname, 'src/main.jsx'),
      name: 'ChatBundle',
      fileName: () => 'chat-bundle.js',
      formats: ['iife'], // 브라우저에서 바로 실행 가능한 자바스크립트 형태
    },
    rollupOptions: {
      output: {

        // 번들 파일 맨 윗줄에 이 코드를 강제 삽입
        banner: 'if (typeof window !== "undefined" && !window.process) { window.process = { env: {} }; window.global = window; }',

        assetFileNames: (assetInfo) => {
          if (assetInfo.name && assetInfo.name.endsWith('.css')) {
            return 'chat-bundle.css';
          }
          return '[name].[ext]';
        },
      },
    },
  },
})