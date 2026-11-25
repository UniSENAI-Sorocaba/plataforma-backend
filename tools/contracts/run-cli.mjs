import { spawnSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { bundlePath, findSpecs } from './catalog.mjs';

const mode = process.argv[2];
if (!['lint', 'bundle'].includes(mode)) throw new Error('Modo inválido de validação');
const specs = findSpecs();
if (!specs.length) throw new Error('Nenhum contrato OpenAPI encontrado');
const outputs = new Set();
const catalog = [];
for (const source of specs) {
  const output = bundlePath(source);
  if (outputs.has(output)) throw new Error('Contratos com nomes de bundle conflitantes');
  outputs.add(output);
  mkdirSync(dirname(output), { recursive: true });
  const args = [resolve('node_modules/@redocly/cli/bin/cli.js'), mode, source];
  if (mode === 'bundle') args.push('--output', output);
  const result = spawnSync(process.execPath, args, { stdio: 'inherit' });
  if (result.status !== 0) process.exit(result.status ?? 1);
  catalog.push({ source, bundle: output.replaceAll('\\', '/') });
}
if (mode === 'bundle') writeFileSync('target/contracts/catalog.json', JSON.stringify(catalog, null, 2));
