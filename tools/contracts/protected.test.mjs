import assert from 'node:assert/strict';
import { once } from 'node:events';
import { readFileSync, writeFileSync } from 'node:fs';
import { test } from 'node:test';
import { createMock } from './mock.mjs';
import { assertResponse, assertSecurity, validator } from './schema.mjs';

const fixture = JSON.parse(readFileSync('tools/contracts/fixtures/protected.openapi.json', 'utf8'));
const path = '/synthetic/scopes/{scope}/records/{id}';

test('mock protegido valida parâmetros/body e serve paginação, 401 e 403 sanitizados', async () => {
  assertSecurity(fixture);
  const server = createMock(fixture);
  server.listen(0, '127.0.0.1');
  await once(server, 'listening');
  const url = `http://127.0.0.1:${server.address().port}/synthetic/scopes/scope-a/records/record-a`;
  const evidence = [];
  try {
    const anonymous = await fetch(url, { headers: { 'X-Roles': 'ADMIN' } });
    assert.equal(anonymous.status, 401);
    assertResponse(fixture, path, 'get', 401, anonymous.headers.get('content-type'), await anonymous.json());
    const headers = { Authorization: 'Bearer synthetic-token', 'X-Correlation-Id': 'request-123' };
    const valid = await fetch(`${url}?page=0&size=1`, { headers });
    assert.equal(valid.status, 200);
    assert.equal(valid.headers.get('x-correlation-id'), 'request-123');
    const page = await valid.json();
    assertResponse(fixture, path, 'get', 200, valid.headers.get('content-type'), page);
    evidence.push({ status: 200, body: page });
    const denied = await fetch(url, { headers: { ...headers, Prefer: 'code=403' } });
    assert.equal(denied.status, 403);
    const error = await denied.json();
    assertResponse(fixture, path, 'get', 403, denied.headers.get('content-type'), error);
    assert.ok(!JSON.stringify(error).includes('synthetic-token'));
    evidence.push({ status: 403, body: error });
    for (const query of ['size=101', 'page=-1', 'size=invalid']) assert.equal((await fetch(`${url}?${query}`, { headers })).status, 400);
    assert.equal((await fetch(url.replace('record-a', 'invalid'), { headers })).status, 400);
    assert.equal((await fetch(url, { method: 'POST', headers })).status, 400);
    assert.equal((await fetch(url, { method: 'POST', headers: { ...headers, 'Content-Type': 'text/plain' }, body: 'invalid' })).status, 415);
    for (const body of ['{', '{"id":1}', '{"id":"record-a","roles":["ADMIN"]}']) {
      assert.equal((await fetch(url, { method: 'POST', headers: { ...headers, 'Content-Type': 'application/json' }, body })).status, 400);
    }
    assert.equal((await fetch(url, { method: 'POST', headers: { ...headers, 'Content-Type': 'application/json' }, body: '{"id":"record-a"}' })).status, 200);
  } finally {
    await new Promise(done => server.close(done));
    writeFileSync('target/contracts/protected-responses.json', JSON.stringify(evidence, null, 2));
  }
});

test('schemas de paginação e ProblemDetail rejeitam alterações incompatíveis do produtor', () => {
  const responses = fixture.paths[path].get.responses;
  const page = validator(fixture, responses['200'].content['application/json'].schema);
  assert.equal(page({ items: [], page: -1, size: 1, totalElements: 0 }), false);
  assert.equal(page({ items: [], page: 0, size: 101, totalElements: 0 }), false);
  const problem = validator(fixture, responses['403'].content['application/problem+json'].schema);
  assert.equal(problem({ status: 403, secret: 'synthetic' }), false);
});
