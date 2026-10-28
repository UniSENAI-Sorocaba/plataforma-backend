import assert from 'node:assert/strict';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';

export const methods = ['get', 'post', 'put', 'patch', 'delete', 'options', 'head', 'trace'];

export function resolve(document, value, seen = new Set()) {
  if (!value?.$ref) return value;
  assert.ok(value.$ref.startsWith('#/'), 'O bundle deve conter somente referências locais');
  assert.ok(!seen.has(value.$ref), `Referência circular de objeto OpenAPI: ${value.$ref}`);
  seen.add(value.$ref);
  const target = value.$ref.slice(2).split('/').reduce((node, key) =>
    node?.[key.replaceAll('~1', '/').replaceAll('~0', '~')], document);
  assert.ok(target, `Referência não resolvida: ${value.$ref}`);
  return resolve(document, target, seen);
}

export function validator(document, schema, options = {}) {
  const ajv = new Ajv2020({ strict: false, allErrors: true, ...options });
  addFormats(ajv);
  // Keep JSON Schema references intact: siblings of $ref are meaningful in 3.1.
  return ajv.compile({ ...schema, components: document.components ?? {} });
}

export function assertResponse(document, path, method, status, mediaType, body) {
  const operation = resolve(document, document.paths[path])?.[method];
  assert.ok(operation, `Operação ausente: ${method} ${path}`);
  const response = resolve(document, operation.responses[String(status)]);
  assert.ok(response, `Resposta não documentada: ${status}`);
  const content = response.content?.[mediaType.split(';')[0]];
  assert.ok(content, `Content-Type não documentado: ${mediaType}`);
  const validate = validator(document, content.schema);
  assert.ok(validate(body), JSON.stringify(validate.errors));
}

export function assertSecurity(document) {
  for (const [path, item] of Object.entries(document.paths)) {
    for (const method of methods) {
      const operation = resolve(document, item)[method];
      if (!operation) continue;
      const security = operation.security ?? document.security;
      assert.ok(Array.isArray(security), `${method} ${path}: declarar security explicitamente`);
      if (security.length === 0) continue;
      assert.ok(security.every(requirement => Object.keys(requirement).length > 0),
        `${method} ${path}: segurança opcional não é permitida em rota protegida`);
      for (const requirement of security) {
        for (const name of Object.keys(requirement)) {
          assert.ok(document.components?.securitySchemes?.[name], `Security scheme ausente: ${name}`);
        }
      }
      for (const status of ['401', '403']) {
        const response = resolve(document, operation.responses[status]);
        assert.ok(response?.content?.['application/problem+json']?.schema,
          `${method} ${path}: declarar ${status} com application/problem+json`);
      }
    }
  }
}
