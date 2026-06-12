import { createRoot } from 'react-dom/client';
import { BrowserRouter as Router } from 'react-router-dom';
import './assets/styles/index.css';
import App from './App';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Provider } from 'react-redux';
import { store, persistor } from './store/store';
import { ThemeProvider } from './contexts/ThemeContext';
import { PersistGate } from 'redux-persist/integration/react';

// PWA 서비스 워커 등록
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register('/sw.js')
      .then((registration) => {
        console.log('ServiceWorker 등록 성공:', registration.scope);
      })
      .catch((error) => {
        console.log('ServiceWorker 등록 실패:', error);
      });
  });
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5 // 5분
    }
  }
});

createRoot(document.getElementById('root')!).render(
  <ThemeProvider>
    <Provider store={store}>
      <PersistGate loading={null} persistor={persistor}>
        <QueryClientProvider client={queryClient}>
          <Router>
            <App />
          </Router>
        </QueryClientProvider>
      </PersistGate>
    </Provider>
  </ThemeProvider>
);
