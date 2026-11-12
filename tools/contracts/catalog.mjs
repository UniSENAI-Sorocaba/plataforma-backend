import { existsSync, readdirSync } from 'node:fs';
import { join, relative, resolve } from 'node:path';

export function isContractRoot(path) {
  return /(^|\/)openapi\.(?:json|ya?ml)$/i.test(path.replaceAll('\\', '/')) || /\.openapi\.(?:json|ya?ml)$/i.test(path);
}

export function findSpecs(directory = 'contracts') {
  if (!existsSync(directory)) return [];
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const path = join(directory, entry.name);
    return entry.isDirectory() ? findSpecs(path) : isContractRoot(path) ? [path.replaceAll('\\', '/')] : [];
  }).sort();
}

export function bundlePath(path) {
  const name = relative(resolve('contracts'), resolve(path));
  if (name.startsWith('..')) throw new Error('Contrato fora de contracts/');
  return join('target/contracts', name.replace(/\.(?:json|ya?ml)$/i, '.json'));
}
