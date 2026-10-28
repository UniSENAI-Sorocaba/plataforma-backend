import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { resolve, sep } from 'node:path';

// BE-003 dependency gate. This deterministic report does not replace semantic review.
const baseline = Object.fromEntries(readFileSync('.governance-baseline', 'utf8').trim().split(/\r?\n/)
  .map(line => line.split('=')));
const requiredContracts = ['CONTRACT-AUTH-001', 'CONTRACT-PI-001', 'CONTRACT-EXT-001', 'CONTRACT-PORTAL-001'];
const failures = [];
const warnings = ['Revisão semântica e aprovação humana da BE-003 continuam obrigatórias.'];
const evidence = ['target/contracts/openapi.json', 'target/contracts/mock-responses.json',
  'target/contracts/catalog.json', 'target/contracts/protected-responses.json',
  'target/contracts/breaking-removedEndpoint.json', 'target/contracts/breaking-requiredInput.json',
  'target/contracts/breaking-changedType.json', 'target/contracts/breaking.json'];
const environment = { ...process.env, GIT_TERMINAL_PROMPT: '0' };
// A separate read-only token is needed in CI if governance is private.
if (process.env.GOVERNANCE_READ_TOKEN) {
  const repositoryHost = 'http.https://github.com/UniSENAI-Sorocaba/plataforma-governance.extraheader';
  environment.GIT_CONFIG_COUNT = '1';
  environment.GIT_CONFIG_KEY_0 = repositoryHost;
  environment.GIT_CONFIG_VALUE_0 = `AUTHORIZATION: basic ${Buffer.from(`x-access-token:${process.env.GOVERNANCE_READ_TOKEN}`).toString('base64')}`;
}
const git = args => spawnSync('git', args, { encoding: 'utf8', timeout: 30_000, env: environment });

try {
  if (!/^https:\/\/github\.com\/[\w.-]+\/[\w.-]+$/.test(baseline.repository)
      || !/^governance-v\d+\.\d+\.\d+$/.test(baseline.tag)
      || !/^[a-f0-9]{40}$/.test(baseline.commit)) {
    throw new Error('Formato inválido da baseline; nenhum valor foi alterado');
  }
  if (process.env.GITHUB_ACTIONS === 'true' && !process.env.GOVERNANCE_READ_TOKEN?.trim()) {
    throw new Error('GOVERNANCE_READ_TOKEN não configurado ou indisponível neste run. '
      + 'O repositório de governança é privado; configure um Actions secret com Contents: read '
      + 'para UniSENAI-Sorocaba/plataforma-governance. O GITHUB_TOKEN do backend não concede esse acesso.');
  }
  const refs = git(['ls-remote', '--tags', baseline.repository,
    `refs/tags/${baseline.tag}`, `refs/tags/${baseline.tag}^{}`]);
  if (refs.status !== 0) {
    if (refs.error?.code === 'ETIMEDOUT') throw new Error('Tempo limite ao consultar a baseline remota');
    throw new Error('Não foi possível acessar a baseline remota. Verifique rede e credenciais Git; '
      + 'no CI, confira validade, acesso ao governance e aprovação organizacional do GOVERNANCE_READ_TOKEN. '
      + 'A existência da tag ainda não pôde ser verificada.');
  }
  const lines = refs.stdout.trim().split(/\r?\n/).filter(Boolean).map(line => line.split(/\s+/));
  const sha = lines.find(([, ref]) => ref.endsWith('^{}'))?.[0] ?? lines[0]?.[0];
  if (!sha) failures.push(`Tag oficial ausente: ${baseline.tag}`);
  else if (sha !== baseline.commit) failures.push('Tag oficial não resolve para o SHA fixado');

  mkdirSync('target', { recursive: true });
  const temporary = mkdtempSync(resolve('target/governance-check-'));
  try {
    if (git(['init', '--bare', temporary]).status !== 0
        || git(['-C', temporary, 'fetch', '--no-tags', '--depth=1', baseline.repository, baseline.commit]).status !== 0) {
      throw new Error('Não foi possível obter o SHA remoto; confirme acesso de leitura ao governance');
    }
    const tree = git(['-C', temporary, 'ls-tree', '-r', '--name-only', baseline.commit, '--', '_KB/contracts/']);
    if (tree.status !== 0) throw new Error('Não foi possível ler a árvore remota da baseline');
    const paths = tree.stdout.split(/\r?\n/);
    for (const id of requiredContracts) {
      if (!paths.some(path => new RegExp(`^${id}[.-]`).test(path.split('/').at(-1)))) {
        failures.push(`Contrato global não publicado na baseline: ${id}`);
      }
    }
  } finally {
    if (!temporary.startsWith(`${resolve('target')}${sep}`)) throw new Error('Diretório temporário fora de target');
    rmSync(temporary, { recursive: true, force: true });
  }
} catch (error) {
  failures.push(error.message);
}

for (const path of evidence) {
  if (!existsSync(path)) failures.push(`Evidência ausente: ${path}`);
}
const report = {
  task: 'BE-003 / https://github.com/UniSENAI-Sorocaba/plataforma-backend/issues/3',
  baseline,
  references: ['ADR-008', 'ADR-009', 'engineering/api-guidelines.md', 'MH-GOV-002', 'MH-QA-002', ...requiredContracts],
  evidence, failures, warnings,
  result: failures.length ? 'FAIL' : 'WARN'
};
mkdirSync('target/contracts', { recursive: true });
writeFileSync('target/contracts/spec-compliance.json', JSON.stringify(report, null, 2));
console.log(JSON.stringify(report, null, 2));
process.exitCode = failures.length ? 1 : 0;
