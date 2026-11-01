const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const collection = JSON.parse(fs.readFileSync(path.join(__dirname, 'gestao-acoes.postman_collection.json'), 'utf8'));
function requests(items) { return items.flatMap(item => item.item ? requests(item.item) : [item]); }

test('todos os scripts publicados são JavaScript válido', () => {
  for (const item of [collection, ...requests(collection.item)]) for (const event of item.event || []) assert.doesNotThrow(() => new Function('pm', event.script.exec.join('\n')), 'Script inválido: ' + (item.name || 'coleção'));
});

test('corpos do fluxo de autenticação continuam JSON válido após substituição segura de variáveis', () => {
  const folder = collection.item.find(item => item.name === 'Autenticação e usuários — ambiente isolado');
  assert.ok(folder);
  for (const item of requests(folder.item)) if (item.request.body?.mode === 'raw') {
    const rendered = item.request.body.raw.replace(/\{\{\w+Json\}\}/g, JSON.stringify('fixture " \\ á')).replace(/\{\{versaoEsperada\}\}/g, '3');
    assert.doesNotThrow(() => JSON.parse(rendered), 'Corpo inválido: ' + item.name);
  }
});

test('coleção publicada oferece fluxo integrado de autenticação, administração e duas confirmações públicas', () => {
  const folder = collection.item.find(item => item.name === 'Autenticação e usuários — ambiente isolado');
  assert.ok(folder, 'Pasta de autenticação ausente');
  const operations = requests(folder.item).map(item => item.request.method + ' ' + (typeof item.request.url === 'string' ? item.request.url : item.request.url.raw));
  for (const operation of ['GET {{baseUrl}}/auth/csrf', 'POST {{baseUrl}}/auth/login', 'GET {{baseUrl}}/auth/me', 'POST {{baseUrl}}/auth/logout', 'POST {{baseUrl}}/usuarios', 'GET {{baseUrl}}/usuarios', 'GET {{baseUrl}}/usuarios/{{usuarioId}}', 'PATCH {{baseUrl}}/usuarios/{{usuarioId}}/perfil', 'PATCH {{baseUrl}}/usuarios/{{usuarioId}}/situacao', 'POST {{baseUrl}}/usuarios/{{usuarioId}}/redefinicoes']) assert.ok(operations.includes(operation), 'Operação ausente: ' + operation);
  assert.equal(operations.filter(operation => operation === 'POST {{baseUrl}}/auth/redefinicoes/confirmar').length, 2);
});

test('exportação não contém credenciais configuradas, Bearer alternativo ou Cookie manual', () => {
  for (const variable of collection.variable || []) if (/senha|password|codigoUsoUnico|csrf|jwt|token/i.test(variable.key)) assert.equal(variable.value, '', 'Variável sensível deve ser vazia: ' + variable.key);
  assert.equal(collection.auth.type, 'noauth', 'Cookie jar deve gerir autenticação');
  for (const item of requests(collection.item)) for (const header of item.request.header || []) assert.ok(!/^(cookie|authorization)$/i.test(header.key), 'Header de credencial manual não permitido');
});
