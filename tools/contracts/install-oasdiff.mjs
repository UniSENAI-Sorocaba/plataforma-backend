import { createHash } from 'node:crypto';
import { mkdirSync, writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { resolve } from 'node:path';

const version = '1.34.1';
const assets = {
  'win32-x64': ['windows_amd64', '15e6f32ece94244534a61ce4fb1d60912e51e67e363a05bb6561d89b28df73bb'],
  'win32-arm64': ['windows_arm64', '4794300521a2ff76d14205f5141884e6f6052212484caf1d3655c149b314492c'],
  'linux-x64': ['linux_amd64', '75775f65b0adebd26a8e013af51a799342dfdc6262d89efa9e3b7bdf89657d90'],
  'linux-arm64': ['linux_arm64', 'e8f99bbf102a4b7f235104da72d0b9d67702e78622d1d4d37169e698ee90a249'],
  'darwin-x64': ['darwin_all', '64948ba30c5555d3d15b95a7f3eda22fa83c15cf37fd4dad1720c0deb0a07cb5'],
  'darwin-arm64': ['darwin_all', '64948ba30c5555d3d15b95a7f3eda22fa83c15cf37fd4dad1720c0deb0a07cb5']
};
const asset = assets[`${process.platform}-${process.arch}`];
if (!asset) throw new Error('Plataforma não suportada pelo instalador de oasdiff');
const filename = `oasdiff_${version}_${asset[0]}.tar.gz`;
const response = await fetch(`https://github.com/oasdiff/oasdiff/releases/download/v${version}/${filename}`,
  { signal: AbortSignal.timeout(60_000) });
if (!response.ok) throw new Error(`Download de oasdiff: HTTP ${response.status}`);
const bytes = Buffer.from(await response.arrayBuffer());
if (createHash('sha256').update(bytes).digest('hex') !== asset[1]) {
  throw new Error('SHA-256 do oasdiff difere do valor versionado');
}
const directory = resolve('target/tools/oasdiff');
mkdirSync(directory, { recursive: true });
const archive = resolve(directory, filename);
writeFileSync(archive, bytes);
const executable = process.platform === 'win32' ? 'oasdiff.exe' : 'oasdiff';
const extraction = spawnSync('tar', ['-xzf', archive, '-C', directory, executable], { encoding: 'utf8' });
if (extraction.status !== 0) throw new Error(extraction.error?.message ?? extraction.stderr);
const result = spawnSync(resolve(directory, executable), ['--version'], { encoding: 'utf8' });
if (result.status !== 0 || !result.stdout.includes(version)) throw new Error('Versão inesperada de oasdiff');
console.log(result.stdout.trim());
