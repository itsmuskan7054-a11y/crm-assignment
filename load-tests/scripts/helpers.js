import http from 'k6/http';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';

export function login(email, password) {
  const res = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    email: email || 'admin@palmonas.com',
    password: password || 'Admin@123',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  if (res.status === 200) {
    const body = JSON.parse(res.body);
    return body.data.accessToken;
  }
  return null;
}

export function authHeaders(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };
}
