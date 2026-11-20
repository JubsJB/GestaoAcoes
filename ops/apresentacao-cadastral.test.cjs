'use strict';
const assert = require('node:assert/strict');
const {spawnSync} = require('node:child_process');
const test = require('node:test');
const {composeArgs, preflight, resourcePlan, validateAttachments, validateCadastralIdentity, validateTopology} =
  require('./apresentacao.cjs');

const mode = 'integracoes-cadastrais';
const transport = 'http-local';
const project = 'gademofc3279fb5761';
const original = 'gademob84f37a3991e';
const id = 'fc3279fb-5761-4789-ac51-3d67796277f5';
const label = 'gestaoacoes.operacional.run';
const cadastralLabel = 'gestaoacoes.apresentacao.cadastral';
const vars = {
  OP_PROJECT:project, OP_RUN_ID:id, OP_PRIVATE_DIR:'C:/privado-teste',
  OP_DB_NAME:'demonstracao', OP_DB_APP_USER:'aplicacao', OP_BOOTSTRAP_USER:'admin_demo',
  OP_POSTGRES_IMAGE:'postgres:teste', OP_BACKEND_IMAGE:'backend:teste', OP_FRONTEND_IMAGE:'frontend:teste',
  OP_DATA_VOLUME:project+'_data', OP_INGRESS_NETWORK:project+'_ingress', OP_API_NETWORK:project+'_api',
  OP_DATABASE_NETWORK:project+'_database', OP_EGRESS_NETWORK:project+'_egress', APP_TRUSTED_PROXY_IP:'172.31.91.10'
};
const state = {project, id, vars, resources:{}};

function render(selectedMode, env = {}, selectedTransport = transport) {
  const args = composeArgs('NUL', ['--profile','bootstrap','config','--format','json'], selectedMode, selectedTransport);
  // Compose config apenas interpola e valida arquivos. Nenhum acesso ao daemon.
  return spawnSync('docker', args, {encoding:'utf8', env:{PATH:process.env.PATH,
    SystemRoot:process.env.SystemRoot, USERPROFILE:process.env.USERPROFILE, ...vars, ...env}});
}
function config(selectedMode, env, selectedTransport) {
  const result = render(selectedMode, env, selectedTransport);
  assert.equal(result.status, 0, result.error?.message || result.stderr);
  return JSON.parse(result.stdout);
}

test('modo cadastral exige manifesto do projeto limpo, redes e volume exatos e HTTP local', () => {
  assert.doesNotThrow(() => validateCadastralIdentity(state, transport));
  assert.throws(() => validateCadastralIdentity(state, 'https'), /exclusivo/);
  assert.throws(() => validateCadastralIdentity({...state, project:original}, transport), /exclusivo/);
  for (const key of ['OP_RUN_ID','OP_PROJECT','OP_DATA_VOLUME','OP_DATABASE_NETWORK','OP_EGRESS_NETWORK']) {
    assert.throws(() => validateCadastralIdentity({...state,vars:{...vars,[key]:'divergente'}}, transport), /exclusivo/);
  }
  assert.throws(() => preflight({GA_CADASTRAL_GUARD:id}), /redirecionar/);
  const offline = resourcePlan(state, 'sem-integracoes');
  const connected = resourcePlan(state, mode);
  assert.deepEqual(connected, [...offline, ['network',vars.OP_EGRESS_NETWORK]]);
  assert.deepEqual(resourcePlan({...state, resources:{[vars.OP_EGRESS_NETWORK]:'registrada'}}, 'sem-integracoes'), connected);
});

test('Compose real preserva os modos antigos e habilita somente os destinos cadastrais', () => {
  assert.notEqual(render(mode).status, 0);
  const offline = config('sem-integracoes');
  const connected = config('com-integracoes');
  const cadastral = config(mode, {GA_CADASTRAL_GUARD:id});
  validateTopology(offline, 'sem-integracoes', transport);
  validateTopology(connected, 'com-integracoes', transport);
  validateTopology(cadastral, mode, transport);
  for (const service of ['frontend','postgres','bootstrap']) {
    assert.deepEqual(cadastral.services[service], offline.services[service]);
  }
  assert.deepEqual(cadastral.volumes, offline.volumes);
  assert.deepEqual(cadastral.secrets, offline.secrets);
  assert.equal(cadastral.services.backend.image, offline.services.backend.image);
  assert.equal(cadastral.services.backend.environment.BRAPI_BASE_URL, offline.services.backend.environment.BRAPI_BASE_URL);
  assert.equal(cadastral.services.backend.environment.ALPHA_VANTAGE_BASE_URL, offline.services.backend.environment.ALPHA_VANTAGE_BASE_URL);
  assert.throws(() => validateTopology(cadastral, 'sem-integracoes', transport), /rede/);
  assert.throws(() => validateTopology(config(mode, {GA_CADASTRAL_GUARD:id, OP_PROJECT:original}), mode, transport), /Identidade/);
  assert.throws(() => validateTopology(config(mode, {GA_CADASTRAL_GUARD:id, OP_DATA_VOLUME:original+'_data'}), mode, transport), /Identidade/);
  const originalEnv = Object.fromEntries(Object.entries(vars).map(([key,value]) => [key,value.replace(project,original)]));
  validateTopology(config('sem-integracoes', originalEnv), 'sem-integracoes', transport);
  validateTopology(config('com-integracoes', originalEnv, 'https'), 'com-integracoes', 'https');
  cadastral.services.backend.secrets.push({source:'brapi_key',target:'integration.brapi.api-key'});
  assert.throws(() => validateTopology(cadastral, mode, transport), /Segredos/);
});

test('verificacao real de anexos recusa chave de mercado e egress residual offline', () => {
  const backend = {
    Config:{Labels:{[label]:id,[cadastralLabel]:id}}, HostConfig:{},
    NetworkSettings:{Networks:{[vars.OP_API_NETWORK]:{},[vars.OP_DATABASE_NETWORK]:{},[vars.OP_EGRESS_NETWORK]:{}}}
  };
  validateAttachments({backend}, state, mode, transport);
  assert.throws(() => validateAttachments({backend}, state, 'sem-integracoes', transport), /rede/);
  backend.Mounts = [{Destination:'/run/secrets/integration.alpha-vantage.api-key'}];
  assert.throws(() => validateAttachments({backend}, state, mode, transport), /montagens/);
  delete backend.NetworkSettings.Networks[vars.OP_EGRESS_NETWORK];
  delete backend.Mounts;
  validateAttachments({backend}, state, 'sem-integracoes', transport);
});
