import { createServer } from 'node:http';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { assertResponse, resolve as resolveReference } from './schema.mjs';

export function createMock(document) {
  return createServer((request, response) => {
    const sendProblem = (status, title) => {
      response.writeHead(status, { 'Content-Type': 'application/problem+json' });
      response.end(JSON.stringify({ type: 'about:blank', title, status }));
    };
    const path = new URL(request.url, 'http://127.0.0.1').pathname;
    const operation = document.paths[path]?.[request.method.toLowerCase()];
    if (!operation) return sendProblem(404, 'Operação ausente no contrato do mock');
    const preferences = Object.fromEntries((request.headers.prefer ?? '').split(',')
      .map(item => item.trim().split('=')));
    const status = preferences.code ?? Object.keys(operation.responses).find(code => /^2\d\d$/.test(code));
    const definition = resolveReference(document, operation.responses[status]);
    if (!definition) return sendProblem(400, 'Status ausente no contrato do mock');
    const [mediaType, media] = Object.entries(definition.content ?? {})[0] ?? [];
    const example = preferences.example ?? Object.keys(media?.examples ?? {})[0];
    const value = media?.examples?.[example]?.value;
    if (value === undefined) return sendProblem(400, 'Exemplo ausente no contrato do mock');
    try {
      assertResponse(document, path, request.method.toLowerCase(), status, mediaType, value);
    } catch {
      return sendProblem(500, 'Exemplo incompatível com o schema do contrato');
    }
    response.writeHead(Number(status), { 'Content-Type': mediaType });
    response.end(JSON.stringify(value));
  });
}

if (process.argv[1] && resolve(process.argv[1]) === import.meta.filename) {
  const document = JSON.parse(readFileSync('target/contracts/openapi.json', 'utf8'));
  const port = Number(process.env.CONTRACT_MOCK_PORT ?? 4010);
  const server = createMock(document);
  server.listen(port, '127.0.0.1', () => console.log(`Mock: http://127.0.0.1:${server.address().port}`));
  server.on('error', error => { console.error(error.message); process.exitCode = 1; });
}
