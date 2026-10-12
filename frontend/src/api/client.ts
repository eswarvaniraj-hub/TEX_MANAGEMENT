function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return decodeURIComponent(parts.pop()!.split(';').shift()!);
  }
  return null;
}

export async function fetchCsrfToken(): Promise<string> {
  const response = await fetch('/api/v1/auth/csrf', { credentials: 'include' });
  if (response.ok) {
    const data = await response.json();
    return data.token;
  }
  return '';
}

export interface RequestOptions extends RequestInit {
  skipAuthRedirect?: boolean;
}

export async function apiClient<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers || {});
  const method = (options.method || 'GET').toUpperCase();

  // If state-changing request, ensure X-XSRF-TOKEN header is sent
  if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
    let csrfToken = getCookie('XSRF-TOKEN');
    if (!csrfToken) {
      csrfToken = await fetchCsrfToken();
    }
    if (csrfToken && !headers.has('X-XSRF-TOKEN')) {
      headers.set('X-XSRF-TOKEN', csrfToken);
    }
    if (!headers.has('Content-Type') && options.body && typeof options.body === 'string') {
      headers.set('Content-Type', 'application/json');
    }
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
    credentials: 'include',
  });

  if (response.status === 401 && !options.skipAuthRedirect && !endpoint.includes('/auth/login')) {
    // Session expired or unauthenticated; signal auth context and route to login page
    window.dispatchEvent(new CustomEvent('auth:unauthorized'));
    if (window.location.pathname !== '/login') {
      window.location.href = '/login?expired=true';
    }
  }

  if (!response.ok) {
    let errorMessage = `Request failed with status ${response.status}`;
    try {
      const errorJson = await response.json();
      errorMessage = errorJson.message || errorJson.error || errorMessage;
    } catch {
      // Body not JSON
    }
    const error = new Error(errorMessage);
    (error as unknown as { status: number }).status = response.status;
    throw error;
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}
