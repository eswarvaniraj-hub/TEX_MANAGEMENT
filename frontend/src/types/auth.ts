export interface User {
  username: string;
  role: string;
  mustChangePassword: boolean;
}

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export interface AuthResponse {
  username: string;
  role: string;
  mustChangePassword: boolean;
}

export interface CsrfResponse {
  token: string;
  headerName: string;
  parameterName: string;
}
