import http from 'k6/http';
import { check, sleep } from 'k6';
import { login, authHeaders, BASE_URL } from './helpers.js';

export const options = {
  stages: [
    { duration: '1m', target: 100 },
    { duration: '2m', target: 150 },
    { duration: '3m', target: 150 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.10'],
  },
};

export function setup() {
  return { token: login() };
}

export default function (data) {
  const opts = authHeaders(data.token);

  const listRes = http.get(`${BASE_URL}/orders?page=0&size=20`, opts);
  check(listRes, {
    'list 200': (r) => r.status === 200,
    'list p95 < 1s': (r) => r.timings.duration < 1000,
  });

  const statsRes = http.get(`${BASE_URL}/orders/stats`, opts);
  check(statsRes, {
    'stats 200': (r) => r.status === 200,
  });

  // Search
  const searchRes = http.get(`${BASE_URL}/orders?search=Kumar&page=0&size=10`, opts);
  check(searchRes, {
    'search 200': (r) => r.status === 200,
  });

  sleep(0.5);
}
