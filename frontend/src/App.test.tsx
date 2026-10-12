import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MantineProvider } from '@mantine/core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App';

describe('App Component', () => {
  it('renders the header and displays UP badge when health check succeeds', async () => {
    // Mock global fetch for /api/v1/health
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ status: 'UP' }),
      })
    );

    const testQueryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={testQueryClient}>
        <MantineProvider>
          <App />
        </MantineProvider>
      </QueryClientProvider>
    );

    expect(screen.getByText('Textile Accounts Management')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByText('UP')).toBeDefined();
    });

    vi.unstubAllGlobals();
  });
});
