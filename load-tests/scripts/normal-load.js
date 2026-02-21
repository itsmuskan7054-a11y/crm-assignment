import http from 'k6/http';
import { check, sleep } from 'k6';
import { login, authHeaders, BASE_URL } from './helpers.js';

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '5m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.05'],
  },
};

let token;

export function setup() {
  token = login();
  return { token };
}

export default function (data) {
  const opts = authHeaders(data.token);

  // List orders with filters
  const listRes = http.get(`${BASE_URL}/orders?page=0&size=20&sortBy=orderedAt&sortDir=desc`, opts);
  check(listRes, {
    'list orders 200': (r) => r.status === 200,
    'list orders < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);

  // Get dashboard stats
  const statsRes = http.get(`${BASE_URL}/orders/stats`, opts);
  check(statsRes, {
    'stats 200': (r) => r.status === 200,
    'stats < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);

  // Get single order
  if (listRes.status === 200) {
    const orders = JSON.parse(listRes.body).data.content;
    if (orders.length > 0) {
      const orderId = orders[Math.floor(Math.random() * orders.length)].id;
      const detailRes = http.get(`${BASE_URL}/orders/${orderId}`, opts);
      check(detailRes, {
        'detail 200': (r) => r.status === 200,
        'detail < 500ms': (r) => r.timings.duration < 500,
      });
    }
  }

  sleep(1);

  // Filter by channel
  const channels = ['AMAZON', 'FLIPKART', 'WEBSITE'];
  const channel = channels[Math.floor(Math.random() * channels.length)];
  const filterRes = http.get(`${BASE_URL}/orders?channel=${channel}&page=0&size=10`, opts);
  check(filterRes, {
    'filter 200': (r) => r.status === 200,
  });

  sleep(1);
}
