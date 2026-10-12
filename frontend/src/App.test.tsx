import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MantineProvider } from '@mantine/core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import App from './App';

describe('App Root Component', () => {
  it('initializes and routes unauthenticated users to the LoginPage', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string) => {
        if (url === '/api/v1/auth/me') {
          return Promise.resolve({
            ok: false,
            status: 401,
            json: async () => ({ error: 'Unauthorized' }),
          });
        }
        return Promise.resolve({ ok: true, json: async () => ({}) });
      })
    );

    const testQueryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={testQueryClient}>
        <MantineProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </MantineProvider>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('TEX_ARUN')).toBeInTheDocument();
      expect(screen.getByText('Owner Sign In')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Sign In/i })).toBeInTheDocument();
    });

    vi.unstubAllGlobals();
  });
});
