import { apiClient } from './client';
import { ChangePasswordPayload, LoginCredentials, User } from '../types/auth';

export async function login(credentials: LoginCredentials): Promise<User> {
  return apiClient<User>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
    skipAuthRedirect: true,
  });
}

export async function logout(): Promise<void> {
  await apiClient<void>('/api/v1/auth/logout', {
    method: 'POST',
  });
}

export async function getCurrentUser(): Promise<User> {
  return apiClient<User>('/api/v1/auth/me', {
    method: 'GET',
    skipAuthRedirect: true,
  });
}

export async function changePassword(payload: ChangePasswordPayload): Promise<User> {
  return apiClient<User>('/api/v1/auth/change-password', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
