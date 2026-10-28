import { createServer } from 'node:http';
import { randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { assertResponse, resolve as dereference, validator } from './schema.mjs';

function match(document, pathname) {
  const actual = pathname.split('/').map(decodeURIComponent);
  const paths = Object.keys(document.paths).sort((a, b) => (a.match(/\{/g)?.length ?? 0) - (b.match(/\{/g)?.length ?? 0));
  for (const path of paths) {
    const expected = path.split('/');
    if (expected.length !== actual.length) continue;
    const variables = {};
    const matches = expected.every((part, index) => {
      if (part.startsWith('{') && part.endsWith('}')) { variables[part.slice(1, -1)] = actual[index]; return true; }
      return part === actual[index];
    });
    if (matches) return { path, variables };
  }
}

function credentialsPresent(document, operation, request, url) {
  const security = operation.security ?? document.security ?? [];
  return security.length === 0 || security.some(requirement => Object.keys(requirement).every(name => {
    const scheme = dereference(document, document.components?.securitySchemes?.[name]);
    if (scheme?.type === 'apiKey') return Boolean(scheme.in === 'header'
      ? request.headers[scheme.name.toLowerCase()] : url.searchParams.get(scheme.name));
    return Boolean(scheme && /^(http|oauth2|openIdConnect)$/.test(scheme.type)
      && /^Bearer\s+\S+$/i.test(request.headers.authorization ?? ''));
  }));
}

export function createMock(document) {
  return createServer(async (request, response) => {
    const rawCorrelation = request.headers['x-correlation-id'];
    const correlation = typeof rawCorrelation === 'string' && /^[A-Za-z0-9._-]{1,64}$/.test(rawCorrelation) ? rawCorrelation : randomUUID();
    response.setHeader('X-Correlation-Id', correlation);
    response.setHeader('Cache-Control', 'no-store');
    const problem = (status, title) => {
      response.writeHead(status, { 'Content-Type': 'application/problem+json' });
      response.end(JSON.stringify({ type: 'about:blank', title, detail: title, status, correlationId: correlation }));
    };
    try {
      const url = new URL(request.url, 'http://127.0.0.1');
      const matched = match(document, url.pathname);
      const method = request.method === 'HEAD' ? 'get' : request.method.toLowerCase();
      const item = matched && dereference(document, document.paths[matched.path]);
      const operation = item?.[method];
      if (!operation) return problem(404, 'Operação ausente no contrato do mock');
      const authenticated = credentialsPresent(document, operation, request, url);
      for (const raw of [...(item.parameters ?? []), ...(operation.parameters ?? [])]) {
        const parameter = dereference(document, raw);
        const value = parameter.in === 'path' ? matched.variables[parameter.name]
          : parameter.in === 'query' ? url.searchParams.get(parameter.name) ?? undefined
            : request.headers[parameter.name.toLowerCase()];
        if (value === undefined) { if (parameter.required) return problem(400, 'Parâmetro obrigatório ausente'); else continue; }
        const check = validator(document, { type: 'object', properties: { value: parameter.schema } }, { coerceTypes: true });
        if (!check({ value })) return problem(400, 'Parâmetro incompatível com o contrato');
      }
      let bytes = Buffer.alloc(0);
      for await (const chunk of request) {
        if (bytes.length + chunk.length > 65_536) return problem(413, 'Body excede limite do mock');
        bytes = Buffer.concat([bytes, chunk]);
      }
      const bodyDefinition = dereference(document, operation.requestBody);
      if (bodyDefinition?.required && bytes.length === 0) return problem(400, 'Body obrigatório ausente');
      if (bytes.length && bodyDefinition) {
        const media = bodyDefinition.content?.[(request.headers['content-type'] ?? '').split(';')[0]];
        if (!media) return problem(415, 'Content-Type ausente no contrato');
        const input = JSON.parse(bytes.toString('utf8'));
        if (!validator(document, media.schema)(input)) return problem(400, 'Body incompatível com o contrato');
      }
      const preferences = Object.fromEntries((request.headers.prefer ?? '').split(',').map(value => value.trim().split('=')));
      const status = authenticated ? preferences.code ?? Object.keys(operation.responses).find(code => /^2\d\d$/.test(code)) : '401';
      const definition = dereference(document, operation.responses[status]);
      if (!definition) return problem(400, 'Status ausente no contrato do mock');
      const [mediaType, media] = Object.entries(definition.content ?? {})[0] ?? [];
      const example = (authenticated ? preferences.example : undefined) ?? Object.keys(media?.examples ?? {})[0];
      const value = media?.examples?.[example]?.value;
      if (value === undefined) return problem(400, 'Exemplo ausente no contrato do mock');
      try { assertResponse(document, matched.path, method, status, mediaType, value); }
      catch { return problem(500, 'Exemplo incompatível com o schema do contrato'); }
      response.writeHead(Number(status), { 'Content-Type': mediaType });
      response.end(request.method === 'HEAD' ? undefined : JSON.stringify(value));
    } catch {
      problem(400, 'Requisição ou exemplo inválido para o mock');
    }
  });
}

if (process.argv[1] && resolve(process.argv[1]) === import.meta.filename) {
  const document = JSON.parse(readFileSync(process.argv[2] ?? 'target/contracts/openapi.json', 'utf8'));
  const server = createMock(document);
  server.listen(Number(process.env.CONTRACT_MOCK_PORT ?? 4010), '127.0.0.1', () => console.log(`Mock: http://127.0.0.1:${server.address().port}`));
  server.on('error', error => { console.error(error.message); process.exitCode = 1; });
}
