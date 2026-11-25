import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { test } from 'node:test';
import { compare } from './breaking.mjs';

const document = JSON.parse(readFileSync('target/contracts/openapi.json', 'utf8'));

test('oasdiff bloqueia remoção, nova entrada obrigatória e mudança incompatível de tipo', () => {
  const directory = mkdtempSync('target/contracts/diff-');
  try {
    const base = resolve(directory, 'base.json');
    writeFileSync(base, JSON.stringify(document));
    const mutations = {
      removedEndpoint(candidate) { delete candidate.paths['/actuator/health']; },
      requiredInput(candidate) {
        candidate.paths['/actuator/health'].get.parameters = [
          { in: 'query', name: 'synthetic', required: true, schema: { type: 'string' } }
        ];
      },
      changedType(candidate) { candidate.components.schemas.Health.properties.status = { type: 'integer' }; }
    };
    for (const [name, mutate] of Object.entries(mutations)) {
      const candidate = structuredClone(document);
      mutate(candidate);
      const file = resolve(directory, `${name}.json`);
      writeFileSync(file, JSON.stringify(candidate));
      const result = compare(base, file);
      assert.equal(result.status, 1, `${name}: ${result.stdout} ${result.stderr}`);
      const report = JSON.parse(result.stdout);
      assert.ok(report.length > 0, `${name}: deve existir diagnóstico de incompatibilidade`);
      writeFileSync(`target/contracts/breaking-${name}.json`, result.stdout);
    }
    assert.equal(compare(base, base).status, 0);
    const compatible = structuredClone(document);
    compatible.info.description += ' Exemplo de documentação compatível.';
    const compatibleFile = resolve(directory, 'compatible.json');
    writeFileSync(compatibleFile, JSON.stringify(compatible));
    assert.equal(compare(base, compatibleFile).status, 0);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
});
