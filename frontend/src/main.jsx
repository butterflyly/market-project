if (typeof window !== 'undefined' && !window.process) {
  window.process = { env: {} };
}

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'

const container = document.getElementById('react-chat-root');

if (container) {
  createRoot(container).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
} else {
  console.error("❌ 'react-chat-root' 엘리먼트를 찾을 수 없습니다.");
}