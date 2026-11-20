'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const {composeArgs, requireTransport, validateFrontendBinding, verifyHttpReceipt, lifecycle} = require('./apresentacao.cjs');

test('HTTP opt-in usa o mesmo projeto e preserva selecao de integracoes', () => {
  assert.equal(requireTransport(undefined), 'https');
  assert.throws(() => requireTransport('outro'), /transporte/);
  for (const modo of ['sem-integracoes','com-integracoes']) {
    const args = composeArgs('runtime.env', ['config'], modo, 'http-local');
    assert.ok(args.some(arg => arg.endsWith('compose.apresentacao-http.yaml')));
    assert.equal(args.some(arg => arg.endsWith('compose.apresentacao-integracoes.yaml')), modo === 'com-integracoes');
  }
  assert.ok(!composeArgs('runtime.env', ['config']).some(arg => arg.endsWith('compose.apresentacao-http.yaml')));
});

test('publicacao real recusa IP externo, IPv6 e segunda porta', () => {
  const loopback = {'8081/tcp': [{HostIp:'127.0.0.1', HostPort:'8081'}]};
  assert.doesNotThrow(() => validateFrontendBinding(loopback, 'http-local'));
  for (const ip of ['0.0.0.0', '::', '192.168.1.1'])
    assert.throws(() => validateFrontendBinding({'8081/tcp':[{HostIp:ip,HostPort:'8081'}]}, 'http-local'), /loopback/);
  assert.throws(() => validateFrontendBinding({...loopback,'8443/tcp':loopback['8081/tcp']}, 'http-local'), /loopback/);
  assert.throws(() => validateFrontendBinding(loopback, 'https'), /loopback/);
});

test('recibo HTTP recusa troca da imagem ou configuracao', () => {
  const expected = {imageId:'sha256:imagem', configHash:'nginx', overrideHash:'compose', dockerfileHash:'dockerfile'};
  assert.doesNotThrow(() => verifyHttpReceipt(expected, {...expected}));
  assert.throws(() => verifyHttpReceipt(undefined, expected), /recibo/);
  for (const key of Object.keys(expected))
    assert.throws(() => verifyHttpReceipt(expected, {...expected,[key]:'alterado'}), /recibo/);
});

test('troca de transporte inicia o PostgreSQL existente e para ambos os serviços web antes da troca', async () => {
  const commands = [];
  await lifecycle('start', {bootstrapCompleted:true}, {
    compose: (...args) => commands.push(args),
    mode:'sem-integracoes', preserveDatabase:true, switching:true,
    waitPg:async()=>{}, verifyDatabase:()=>{}, verifyAttachments:()=>{}, waitReady:async()=>{}
  });
  assert.deepEqual(commands[0], ['start','postgres']);
  assert.deepEqual(commands[1], ['stop','frontend','backend']);
  assert.ok(commands.slice(2).every(args => !args.includes('postgres') && !args.includes('bootstrap')));
});
