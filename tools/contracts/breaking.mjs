import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, relative, resolve, sep } from 'node:path';
import { findSpecs, isContractRoot } from './catalog.mjs';

export const binary = resolve('target/tools/oasdiff', process.platform === 'win32' ? 'oasdiff.exe' : 'oasdiff');

export function compare(base, revision) {
  if (!existsSync(binary)) throw new Error('Execute npm run contracts:install-diff primeiro');
  const result = spawnSync(binary, ['breaking', base, revision, '--fail-on', 'WARN', '--format', 'json'],
    { encoding: 'utf8', timeout: 60_000 });
  if (result.error) throw result.error;
  return result;
}

export function compareRepository(base, cwd = process.cwd()) {
  const git = args => spawnSync('git', args, { cwd, encoding: 'utf8', timeout: 30_000, maxBuffer: 10 * 1024 * 1024 });
  const revision = git(['rev-parse', '--verify', '--end-of-options', `${base}^{commit}`]);
  if (revision.status !== 0) throw new Error(`Referência base inválida: ${base}`);
  const sha = revision.stdout.trim();
  const tree = git(['ls-tree', '-r', '-z', '--name-only', sha, '--', 'contracts']);
  if (tree.status !== 0) throw new Error('Não foi possível inventariar os contratos da base');
  const files = tree.stdout.split('\0').filter(Boolean);
  const oldRoots = files.filter(isContractRoot);
  const currentRoots = findSpecs(resolve(cwd, 'contracts')).map(path => relative(cwd, path).replaceAll('\\', '/'));
  const roots = [...new Set([...oldRoots, ...currentRoots])].sort();
  if (!roots.length) throw new Error('Nenhum contrato disponível na base ou no candidato');
  mkdirSync('target/contracts', { recursive: true });
  const directory = mkdtempSync(resolve('target/contracts/base-'));
  const comparisons = [];
  let status = 0;
  try {
    // Reconstruct every supporting file from the immutable base, including relative refs.
    for (const path of files) {
      const output = resolve(directory, path);
      if (!output.startsWith(`${directory}${sep}`)) throw new Error('Arquivo da base fora do diretório temporário');
      const contents = git(['show', `${sha}:${path}`]);
      if (contents.status !== 0) throw new Error('Não foi possível ler um arquivo do contrato base');
      mkdirSync(dirname(output), { recursive: true });
      writeFileSync(output, contents.stdout);
    }
    for (const source of roots) {
      if (!currentRoots.includes(source)) {
        comparisons.push({ source, result: 'removed', exitCode: 1 });
        status = 1;
      } else if (!oldRoots.includes(source)) {
        comparisons.push({ source, result: 'initial', exitCode: 0 });
      } else {
        const result = compare(resolve(directory, source), resolve(cwd, source));
        comparisons.push({ source, result: 'compared', exitCode: result.status,
          changes: result.stdout.trim() ? JSON.parse(result.stdout) : [], diagnostic: result.stderr.trim() });
        if (result.status !== 0) status = result.status ?? 1;
      }
    }
  } finally {
    if (!directory.startsWith(`${resolve('target/contracts')}${sep}`)) throw new Error('Diretório temporário inválido');
    rmSync(directory, { recursive: true, force: true });
  }
  return { status, base: sha, comparisons };
}

if (process.argv[1] && resolve(process.argv[1]) === import.meta.filename) {
  const report = compareRepository(process.argv[2] ?? 'origin/main');
  writeFileSync('target/contracts/breaking.json', JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report, null, 2));
  process.exitCode = report.status;
}
