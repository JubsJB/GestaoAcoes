const {test} = require('node:test');
const assert = require('node:assert/strict');
const {preflight, owned} = require('./homologacao.cjs');
test('recusa execução sem opt-in antes de chamar Docker', () => {
  assert.throws(() => preflight({}), /opt-in/);
});
test('nunca aceita recurso de outra execução para descarte', () => {
  assert.throws(() => owned({Labels:{'gestaoacoes.operacional.run':'outra'}}, 'esperada'), /propriedade/);
  assert.doesNotThrow(() => owned({Labels:{'gestaoacoes.operacional.run':'esperada'}}, 'esperada'));
});
