import http from 'k6/http';
import { check, sleep } from 'k6';
import { login, authHeaders, BASE_URL } from './helpers.js';

export const options = {
  stages: [
    { duration: '1m', target: 100 },
    { duration: '2m', target: 200 },
    { duration: '2m', target: 300 },
    { duration: '2m', target: 400 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(99)<2000'],
    http_req_failed: ['rate<0.20'],
  },
};

export function setup() {
  return { token: login() };
}

export default function (data) {
  const opts = authHeaders(data.token);

  // Mixed workload to stress the system
  const r = Math.random();

  if (r < 0.4) {
    // 40% - List orders
    const res = http.get(`${BASE_URL}/orders?page=${Math.floor(Math.random() * 3)}&size=20`, opts);
    check(res, { 'list ok': (r) => r.status === 200 || r.status === 429 });
  } else if (r < 0.7) {
    // 30% - Dashboard stats
    const res = http.get(`${BASE_URL}/orders/stats`, opts);
    check(res, { 'stats ok': (r) => r.status === 200 || r.status === 429 });
  } else if (r < 0.9) {
    // 20% - Search
    const terms = ['Kumar', 'Sharma', 'amazon', 'Bengaluru', 'Mumbai'];
    const term = terms[Math.floor(Math.random() * terms.length)];
    const res = http.get(`${BASE_URL}/orders?search=${term}`, opts);
    check(res, { 'search ok': (r) => r.status === 200 || r.status === 429 });
  } else {
    // 10% - Health check
    const res = http.get(`${BASE_URL}/health`);
    check(res, { 'health ok': (r) => r.status === 200 });
  }

  sleep(0.2);
}
