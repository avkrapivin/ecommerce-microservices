import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import { AppProviders } from './shared/providers/AppProviders';
import { AppRouter } from './shared/router';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AppProviders>
      <AppRouter />
    </AppProviders>
  </React.StrictMode>,
);
