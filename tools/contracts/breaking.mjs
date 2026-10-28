import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

export const binary = resolve('target/tools/oasdiff', process.platform === 'win32' ? 'oasdiff.exe' : 'oasdiff');

export function compare(base, revision) {
  if (!existsSync(binary)) throw new Error('Execute npm run contracts:install-diff primeiro');
  const result = spawnSync(binary, ['breaking', base, revision, '--fail-on', 'WARN', '--format', 'json'],
    { encoding: 'utf8', timeout: 60_000 });
  if (result.error) throw result.error;
  return result;
}

if (process.argv[1] && resolve(process.argv[1]) === import.meta.filename) {
  const base = process.argv[2] ?? 'origin/main';
  const filename = 'contracts/openapi.yaml';
  // A missing ref must fail, rather than being confused with the initial contract.
  const revision = spawnSync('git', ['rev-parse', '--verify', `${base}^{commit}`], { encoding: 'utf8' });
  if (revision.status !== 0) throw new Error(`Referência base inválida: ${base}`);
  const old = spawnSync('git', ['show', `${base}:${filename}`], { encoding: 'utf8' });
  mkdirSync('target/contracts', { recursive: true });
  if (old.status !== 0) {
    const tree = spawnSync('git', ['ls-tree', '--name-only', base, '--', filename], { encoding: 'utf8' });
    if (tree.status !== 0 || tree.stdout.trim()) throw new Error(old.stderr);
    if (!existsSync(filename)) throw new Error('Contrato ausente tanto na base quanto no candidato');
    console.log('Contrato inicial: não existe versão anterior. Testes negativos continuam obrigatórios.');
    writeFileSync('target/contracts/breaking.json', JSON.stringify({ initialContract: true, base }, null, 2));
  } else {
    if (!existsSync(filename)) throw new Error('Remoção do contrato é incompatível');
    writeFileSync('target/contracts/base.yaml', old.stdout);
    const result = compare('target/contracts/base.yaml', filename);
    writeFileSync('target/contracts/breaking.json', result.stdout || result.stderr);
    process.stdout.write(result.stdout);
    process.stderr.write(result.stderr);
    process.exitCode = result.status ?? 1;
  }
}
