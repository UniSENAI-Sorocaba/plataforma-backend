import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { copyFileSync, mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { resolve, sep } from 'node:path';
import { test } from 'node:test';

test('CI sem secret de governança falha com instrução explícita e preserva relatório', () => {
  const temporary = mkdtempSync(resolve('target/contracts/access-'));
  try {
    copyFileSync('.governance-baseline', resolve(temporary, '.governance-baseline'));
    const result = spawnSync(process.execPath, [resolve('tools/contracts/spec-compliance.mjs')], {
      cwd: temporary,
      encoding: 'utf8',
      timeout: 5_000,
      env: { ...process.env, GITHUB_ACTIONS: 'true', GOVERNANCE_READ_TOKEN: '' }
    });
    assert.equal(result.status, 1, result.stderr);
    const report = JSON.parse(readFileSync(resolve(temporary, 'target/contracts/spec-compliance.json'), 'utf8'));
    assert.equal(report.result, 'FAIL');
    assert.match(report.failures[0], /GOVERNANCE_READ_TOKEN não configurado/);
    assert.match(report.failures[0], /Contents: read/);
    assert.match(report.failures[0], /GITHUB_TOKEN do backend/);
    assert.ok(!report.failures.some(message => message.startsWith('Tag oficial ausente')),
      'Falha de acesso não permite concluir que a tag está ausente');
  } finally {
    assert.ok(temporary.startsWith(`${resolve('target/contracts')}${sep}`));
    rmSync(temporary, { recursive: true, force: true });
  }
});
