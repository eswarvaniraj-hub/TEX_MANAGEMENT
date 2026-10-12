import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MantineProvider } from '@mantine/core';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AppLayout } from '../components/layout/AppLayout';
import { AuthProvider } from '../context/AuthContext';

describe('AppLayout Component', () => {
  it('renders header branding, navigation links, and user pill', async () => {
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
          <MemoryRouter initialEntries={['/']}>
            <Routes>
              <Route element={<AppLayout />}>
                <Route path="/" element={<div>Dashboard Child</div>} />
              </Route>
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </MantineProvider>
    );

    expect(screen.getByText('TEX_ARUN')).toBeInTheDocument();
    expect(screen.getByText('FY 2026-27')).toBeInTheDocument();
    expect(screen.getByText('Dashboard Child')).toBeInTheDocument();

    // Verify key navigation items
    expect(screen.getByRole('link', { name: /Dashboard/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Sales Invoices/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Purchases/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Payments & Receipts/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Expenses/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Banking & Cash/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Parties & GST/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Item Masters/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /General Ledger/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Reports & GST Filing/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /System Settings/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Security Audit Log/i })).toBeInTheDocument();

    // Verify user pill
    await waitFor(() => {
      expect(screen.getByText('owner')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Logout/i })).toBeInTheDocument();
    });
  });
});
