import { after, before, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createMock } from './mock.mjs';
import { assertResponse, validator } from './schema.mjs';

const contract = JSON.parse(readFileSync('target/contracts/openapi.json', 'utf8'));
const mock = createMock(contract);
let origin;

before(async () => {
  await new Promise(resolve => mock.listen(0, '127.0.0.1', resolve));
  origin = 'http://127.0.0.1:' + mock.address().port;
});

after(() => new Promise((resolve, reject) => mock.close(error => error ? reject(error) : resolve())));

test('consumidor HTTP lê paginação e trata recurso ausente', async () => {
  const page = await fetch(origin + '/api/v1/projects?page=0&size=20');
  assert.equal(page.status, 200);
  const body = await page.json();
  assertResponse(contract, '/api/v1/projects', 'get', 200, page.headers.get('content-type'), body);
  assert.deepEqual(body, { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

  const missing = await fetch(origin + '/api/v1/projects/00000000-0000-4000-8000-000000000001', {
    headers: { Prefer: 'code=404, example=notFound' }
  });
  assert.equal(missing.status, 404);
  const problem = await missing.json();
  assertResponse(contract, '/api/v1/projects/{id}', 'get', 404, missing.headers.get('content-type'), problem);
  assert.equal(problem.code, 'NOT_FOUND');
  assert.equal(problem.correlation_id, 'consumer-42');
});

test('proposta de login valida credenciais de entrada e resposta bearer', () => {
  const ref = name => validator(contract, { $ref: '#/components/schemas/' + name });
  const request = ref('LoginRequest');
  assert.ok(request({ email: 'aluno@example.test', password: 'synthetic' }));
  assert.equal(request({ email: 'invalid', password: 'synthetic' }), false);

  const response = ref('LoginResponse');
  assert.ok(response({ access_token: 'synthetic', token_type: 'Bearer' }));
  assert.equal(response({ token_type: 'Bearer' }), false);
});
