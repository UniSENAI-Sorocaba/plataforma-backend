import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { assertResponse, assertSecurity, methods, resolve } from './schema.mjs';

const document = JSON.parse(readFileSync('target/contracts/openapi.json', 'utf8'));
const fixtures = JSON.parse(readFileSync('contracts/fixtures/operational.json', 'utf8'));

test('contrato utiliza OpenAPI 3.1 e declara segurança de cada operação', () => {
  assert.match(document.openapi, /^3\.1\./);
  assertSecurity(document);
});

test('todas as respostas e exemplos têm fixtures válidas e independentes', () => {
  for (const [path, item] of Object.entries(document.paths)) {
    for (const method of methods) {
      if (!item[method]) continue;
      for (const [status, raw] of Object.entries(item[method].responses)) {
        const response = resolve(document, raw);
        for (const [mediaType, media] of Object.entries(response.content ?? {})) {
          assert.ok(Object.keys(media.examples ?? {}).length, `${method} ${path} ${status}: exemplos ausentes`);
          for (const [name, example] of Object.entries(media.examples)) {
            const fixture = fixtures.find(value => value.path === path && value.method === method
              && String(value.status) === status && value.example === name);
            assert.ok(fixture, `Fixture ausente: ${method} ${path} ${status} ${name}`);
            assert.deepEqual(fixture.body, example.value);
            assertResponse(document, path, method, status, mediaType, fixture.body);
          }
        }
      }
    }
  }
});

test('validação rejeita resposta incompatível do produtor', () => {
  for (const body of [{}, { status: 1 }, { status: 'INVALID' }, { status: 'UP', secret: 'synthetic' }]) {
    assert.throws(() => assertResponse(document, '/actuator/health', 'get', 200,
      'application/vnd.spring-boot.actuator.v3+json', body));
  }
});

test('gate de segurança rejeita rota protegida sem 401/403 e aceita declaração completa', () => {
  const protectedDocument = {
    components: { securitySchemes: { bearerAuth: { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' } } },
    security: [{ bearerAuth: [] }],
    paths: { '/synthetic': { get: { responses: { '200': { description: 'Synthetic' } } } } }
  };
  assert.throws(() => assertSecurity(protectedDocument), /401/);
  const problem = { description: 'Synthetic', content: { 'application/problem+json': { schema: { type: 'object' } } } };
  protectedDocument.paths['/synthetic'].get.responses['401'] = problem;
  assert.throws(() => assertSecurity(protectedDocument), /403/);
  protectedDocument.paths['/synthetic'].get.responses['403'] = problem;
  assert.doesNotThrow(() => assertSecurity(protectedDocument));
  protectedDocument.paths['/synthetic'].get.security = [{ unknown: [] }];
  assert.throws(() => assertSecurity(protectedDocument), /Security scheme ausente/);
  protectedDocument.paths['/synthetic'].get.security = [{}];
  assert.throws(() => assertSecurity(protectedDocument), /opcional/);
  delete protectedDocument.paths['/synthetic'].get.security;
  delete protectedDocument.security;
  assert.throws(() => assertSecurity(protectedDocument), /explicitamente/);
});
