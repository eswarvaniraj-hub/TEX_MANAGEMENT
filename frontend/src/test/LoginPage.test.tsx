import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MantineProvider } from '@mantine/core';
import { MemoryRouter } from 'react-router-dom';
import { LoginPage } from '../pages/auth/LoginPage';
import { AuthProvider } from '../context/AuthContext';

describe('LoginPage Component', () => {
  beforeEach(() => {
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
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({}),
        });
      })
    );
  });

  it('renders login form elements and single-owner badge', async () => {
    render(
      <MantineProvider>
        <AuthProvider>
          <MemoryRouter initialEntries={['/login']}>
            <LoginPage />
          </MemoryRouter>
        </AuthProvider>
      </MantineProvider>
    );

    expect(screen.getByText('TEX_ARUN')).toBeInTheDocument();
    expect(screen.getByText('Owner Sign In')).toBeInTheDocument();
    expect(screen.getByText('Single Owner Mode')).toBeInTheDocument();
    expect(screen.getByLabelText(/Username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign In/i })).toBeInTheDocument();
  });

  it('displays error alert banner on failed credentials', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string) => {
        if (url === '/api/v1/auth/login') {
          return Promise.resolve({
            ok: false,
            status: 401,
            json: async () => ({ message: 'Invalid username or password' }),
          });
        }
        return Promise.resolve({
          ok: false,
          status: 401,
          json: async () => ({}),
        });
      })
    );

    render(
      <MantineProvider>
        <AuthProvider>
          <MemoryRouter initialEntries={['/login']}>
            <LoginPage />
          </MemoryRouter>
        </AuthProvider>
      </MantineProvider>
    );

    fireEvent.change(screen.getByLabelText(/Username/i), { target: { value: 'owner' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'WrongPass123!' } });
    fireEvent.click(screen.getByRole('button', { name: /Sign In/i }));

    await waitFor(() => {
      expect(screen.getByText('Invalid username or password')).toBeInTheDocument();
    });
  });
});
