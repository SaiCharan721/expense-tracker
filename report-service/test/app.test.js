import { after, before, test } from 'node:test';
import assert from 'node:assert/strict';
import jwt from 'jsonwebtoken';
import { createApp } from '../src/app.js';

const secret = Buffer.from('dGVzdC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItaHMyNTY=', 'base64');
const fixedNow = () => new Date('2026-10-15T10:00:00Z');

const data = [
  { userId: 'alice', date: new Date('2026-10-02T00:00:00Z'), category: 'FOOD', description: 'Lunch', paymentMethod: 'CARD', tags: [], amount: 12.5 },
  { userId: 'alice', date: new Date('2026-09-12T00:00:00Z'), category: 'RENT', description: 'Rent', paymentMethod: 'BANK_TRANSFER', tags: [], amount: 1350 },
  { userId: 'bob', date: new Date('2026-10-04T00:00:00Z'), category: 'FOOD', description: 'Bob lunch', paymentMethod: 'CASH', tags: [], amount: 9 },
];

// in-memory stand-in for the Mongo repository
const fakeRepo = {
  async findByUserBetween(userId, from, to) {
    return data.filter((e) => e.userId === userId && e.date >= from && e.date < to);
  },
};

let server;
let baseUrl;

before(async () => {
  const app = createApp({ repo: fakeRepo, jwtSecret: secret, corsOrigins: ['http://localhost:5173'], now: fixedNow });
  await new Promise((resolve) => {
    server = app.listen(0, resolve);
  });
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(() => server.close());

const tokenFor = (userId, opts = {}) =>
  jwt.sign({ sub: userId, email: `${userId}@example.com` }, secret, { algorithm: 'HS384', expiresIn: '1h', ...opts });

const get = (path, token, headers = {}) =>
  fetch(baseUrl + path, { headers: { ...(token && { Authorization: `Bearer ${token}` }), ...headers } });

test('health check does not need a token', async () => {
  const res = await get('/health');
  assert.equal(res.status, 200);
  assert.deepEqual(await res.json(), { status: 'UP' });
});

test('reports need a valid token', async () => {
  assert.equal((await get('/reports/trend')).status, 401);
  assert.equal((await get('/reports/trend', 'nope')).status, 401);

  const expired = tokenFor('alice', { expiresIn: -10 });
  assert.equal((await get('/reports/trend', expired)).status, 401);

  const wrongKey = jwt.sign({ sub: 'alice' }, 'some-other-secret');
  assert.equal((await get('/reports/trend', wrongKey)).status, 401);
});

test('monthly CSV only has the signed-in user\'s expenses', async () => {
  const res = await get('/reports/monthly.csv?month=2026-10', tokenFor('alice'));
  assert.equal(res.status, 200);
  assert.match(res.headers.get('content-type'), /text\/csv/);
  assert.match(res.headers.get('content-disposition'), /expenses-2026-10\.csv/);

  const body = await res.text();
  assert.match(body, /2026-10-02,FOOD,Lunch,CARD,,12\.50/);
  assert.doesNotMatch(body, /Bob lunch/);
  assert.doesNotMatch(body, /Rent/);
});

test('monthly CSV defaults to the current month and validates the month', async () => {
  const res = await get('/reports/monthly.csv', tokenFor('alice'));
  assert.match(res.headers.get('content-disposition'), /expenses-2026-10\.csv/);

  assert.equal((await get('/reports/monthly.csv?month=10-2026', tokenFor('alice'))).status, 400);
});

test('trend returns one entry per month', async () => {
  const res = await get('/reports/trend?months=2', tokenFor('alice'));
  assert.equal(res.status, 200);
  assert.deepEqual(await res.json(), {
    months: [
      { month: '2026-09', total: 1350, byCategory: { RENT: 1350 } },
      { month: '2026-10', total: 12.5, byCategory: { FOOD: 12.5 } },
    ],
  });

  assert.equal((await get('/reports/trend?months=0', tokenFor('alice'))).status, 400);
  assert.equal((await get('/reports/trend?months=24', tokenFor('alice'))).status, 400);
});

test('CORS headers only for allowed origins', async () => {
  const allowed = await get('/health', null, { Origin: 'http://localhost:5173' });
  assert.equal(allowed.headers.get('access-control-allow-origin'), 'http://localhost:5173');

  const other = await get('/health', null, { Origin: 'https://evil.example' });
  assert.equal(other.headers.get('access-control-allow-origin'), null);
});
