import assert from 'node:assert/strict';
import { once } from 'node:events';
import { readFileSync, writeFileSync } from 'node:fs';
import { test } from 'node:test';
import { createMock } from './mock.mjs';
import { assertResponse } from './schema.mjs';

test('consumidor recebe todos os exemplos de sucesso e erro por HTTP', { timeout: 15_000 }, async () => {
  const document = JSON.parse(readFileSync('target/contracts/openapi.json', 'utf8'));
  const fixtures = JSON.parse(readFileSync('contracts/fixtures/operational.json', 'utf8'));
  const server = createMock(document);
  server.listen(0, '127.0.0.1');
  await once(server, 'listening');
  const base = `http://127.0.0.1:${server.address().port}`;
  const evidence = [];
  try {
    for (const fixture of fixtures) {
      const response = await fetch(`${base}${fixture.path}`, {
        headers: { Prefer: `code=${fixture.status}, example=${fixture.example}` },
        signal: AbortSignal.timeout(3_000)
      });
      assert.equal(response.status, fixture.status);
      const body = await response.json();
      assert.deepEqual(body, fixture.body);
      assertResponse(document, fixture.path, fixture.method, response.status, response.headers.get('content-type'), body);
      evidence.push({ path: fixture.path, status: response.status, body });
    }
    for (const [path, headers, status] of [
      ['/absent', {}, 404],
      ['/actuator/health', { Prefer: 'code=418' }, 400],
      ['/actuator/health', { Prefer: 'example=absent' }, 400]
    ]) {
      assert.equal((await fetch(`${base}${path}`, { headers, signal: AbortSignal.timeout(3_000) })).status, status);
    }
  } finally {
    await new Promise(resolveClose => server.close(resolveClose));
    writeFileSync('target/contracts/mock-responses.json', JSON.stringify(evidence, null, 2));
  }
});
