import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { resolve, sep } from 'node:path';
import { test } from 'node:test';
import { bundlePath, findSpecs, isContractRoot } from './catalog.mjs';
import { compareRepository } from './breaking.mjs';
import { assertSecurity, methods, resolve as resolveReference, validator } from './schema.mjs';

test('descobre todos os contratos e ignora componentes/fixtures que não são roots', () => {
  assert.ok(isContractRoot('contracts/openapi.yaml'));
  assert.ok(isContractRoot('contracts/impact.openapi.json'));
  assert.ok(!isContractRoot('contracts/fixtures/response.json'));
  assert.equal(bundlePath('contracts/impact.openapi.json').replaceAll('\\', '/'), 'target/contracts/impact.openapi.json');
  assert.throws(() => bundlePath('../outside.openapi.json'), /fora/);
  assert.deepEqual(findSpecs('target/nonexistent-directory'), []);
});

test('todos os bundles do catálogo validam segurança e exemplos de cada schema', () => {
  const catalog = JSON.parse(readFileSync('target/contracts/catalog.json', 'utf8'));
  assert.equal(catalog.length, findSpecs().length);
  for (const entry of catalog) {
    const document = JSON.parse(readFileSync(entry.bundle, 'utf8'));
    assertSecurity(document);
    for (const item of Object.values(document.paths)) {
      for (const method of methods) {
        const operation = resolveReference(document, item)[method];
        if (!operation) continue;
        for (const raw of Object.values(operation.responses)) {
          for (const media of Object.values(resolveReference(document, raw).content ?? {})) {
            for (const example of Object.values(media.examples ?? {})) {
              assert.ok(validator(document, media.schema)(example.value), `${entry.source}: exemplo inválido`);
            }
          }
        }
      }
    }
  }
});

test('comparação Git cobre segundo contrato, remoção e referência base inválida', () => {
  const directory = mkdtempSync(resolve('target/contracts/repository-'));
  const git = args => {
    const result = spawnSync('git', args, { cwd: directory, encoding: 'utf8' });
    assert.equal(result.status, 0, result.stderr);
    return result.stdout.trim();
  };
  try {
    git(['init']);
    mkdirSync(resolve(directory, 'contracts'));
    const document = JSON.parse(readFileSync('target/contracts/openapi.json', 'utf8'));
    writeFileSync(resolve(directory, 'contracts/openapi.json'), JSON.stringify(document));
    writeFileSync(resolve(directory, 'contracts/second.openapi.json'), JSON.stringify(document));
    git(['add', 'contracts']);
    git(['-c', 'user.name=Synthetic', '-c', 'user.email=synthetic@example.invalid', 'commit', '-m', 'synthetic baseline']);
    assert.equal(compareRepository('HEAD', directory).status, 0);
    const changed = structuredClone(document);
    delete changed.paths['/actuator/health'];
    writeFileSync(resolve(directory, 'contracts/second.openapi.json'), JSON.stringify(changed));
    assert.equal(compareRepository('HEAD', directory).status, 1);
    rmSync(resolve(directory, 'contracts/second.openapi.json'));
    const removed = compareRepository('HEAD', directory);
    assert.equal(removed.status, 1);
    assert.ok(removed.comparisons.some(entry => entry.result === 'removed'));
    assert.throws(() => compareRepository('nonexistent-revision', directory), /inválida/);
  } finally {
    assert.ok(directory.startsWith(`${resolve('target/contracts')}${sep}`));
    rmSync(directory, { recursive: true, force: true });
  }
});

test('ciclos de referências de objetos OpenAPI falham explicitamente', () => {
  const document = { a: { $ref: '#/b' }, b: { $ref: '#/a' } };
  assert.throws(() => resolveReference(document, document.a), /circular/);
});
