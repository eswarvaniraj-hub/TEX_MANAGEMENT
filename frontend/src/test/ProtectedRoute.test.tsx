import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MantineProvider } from '@mantine/core';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '../components/auth/ProtectedRoute';
import { AuthProvider } from '../context/AuthContext';

describe('ProtectedRoute Component', () => {
  it('redirects to /login when unauthenticated', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
        json: async () => ({ error: 'Unauthorized' }),
      })
    );

    render(
      <MantineProvider>
        <AuthProvider>
          <MemoryRouter initialEntries={['/dashboard']}>
            <Routes>
              <Route path="/login" element={<div>Login Page Target</div>} />
              <Route element={<ProtectedRoute />}>
                <Route path="/dashboard" element={<div>Protected Dashboard Content</div>} />
              </Route>
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </MantineProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Login Page Target')).toBeInTheDocument();
    });
  });

  it('renders child routes when user is authenticated', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string) => {
        if (url === '/api/v1/auth/me') {
          return Promise.resolve({
            ok: true,
            status: 200,
            json: async () => ({
              username: 'owner',
              role: 'OWNER',
              mustChangePassword: false,
            }),
          });
        }
        return Promise.resolve({ ok: true, json: async () => ({}) });
      })
    );

    render(
      <MantineProvider>
        <AuthProvider>
          <MemoryRouter initialEntries={['/dashboard']}>
            <Routes>
              <Route path="/login" element={<div>Login Page Target</div>} />
              <Route element={<ProtectedRoute />}>
                <Route path="/dashboard" element={<div>Protected Dashboard Content</div>} />
              </Route>
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </MantineProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Protected Dashboard Content')).toBeInTheDocument();
    });
  });

  it('displays Password Change Required modal when mustChangePassword is true', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string) => {
        if (url === '/api/v1/auth/me') {
          return Promise.resolve({
            ok: true,
            status: 200,
            json: async () => ({
              username: 'owner',
              role: 'OWNER',
              mustChangePassword: true,
            }),
          });
        }
        return Promise.resolve({ ok: true, json: async () => ({}) });
      })
    );

    render(
      <MantineProvider>
        <AuthProvider>
          <MemoryRouter initialEntries={['/dashboard']}>
            <Routes>
              <Route element={<ProtectedRoute />}>
                <Route path="/dashboard" element={<div>Protected Dashboard Content</div>} />
              </Route>
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </MantineProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Password Change Required')).toBeInTheDocument();
      expect(screen.getByLabelText(/Current Password/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/^New Password/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Update Master Password/i })).toBeInTheDocument();
    });
  });
});
