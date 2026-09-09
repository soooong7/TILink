import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 백엔드에 CORS 설정이 없어서 dev 서버가 /api 요청을 대신 전달한다.
// 브라우저 입장에서는 같은 출처라 프리플라이트 자체가 발생하지 않는다.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // 백엔드는 8090 을 쓴다 (8080 은 이 머신의 다른 컨테이너가 점유).
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
    },
  },
})
