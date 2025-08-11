import { PropsWithChildren, useMemo } from 'react';
import { ConfigProvider, theme as antdTheme } from 'antd';
import enUS from 'antd/locale/en_US';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// Create a single QueryClient instance for the app
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 30_000,
    },
  },
});

export function AppProviders({ children }: PropsWithChildren) {
  // Configure AntD theme (can be extended later)
  const theme = useMemo(
    () => ({
      algorithm: antdTheme.defaultAlgorithm,
      token: { colorPrimary: '#1677ff' },
    }),
    [],
  );

  return (
    <ConfigProvider locale={enUS} theme={theme}>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </ConfigProvider>
  );
}
