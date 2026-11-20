'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {externalDirectory, preflight, subnetRange, overlaps, owned, composeArgs, lifecycle,
  PINS, certificateArgs, bootstrapAdmin, requireMode, validateTopology, switchMode,
  validateAttachments, resourcePlan} = require('./apresentacao.cjs');

test('materiais privados exigem caminho absoluto fora do repositorio e de seus ancestrais', () => {
  assert.throws(() => externalDirectory('.', 'C:/repo'), /absoluto/);
  for (const location of ['C:/repo', 'C:/repo/secrets', 'C:/'])
    assert.throws(() => externalDirectory(location, 'C:/repo'), /externo/);
  assert.doesNotThrow(() => externalDirectory('C:/privado/demo', 'C:/repo'));
});
test('ambiente nao pode redirecionar Spring, Java, Compose ou Docker', () => {
  for (const key of ['COMPOSE_FILE', 'DOCKER_HOST', 'SPRING_CONFIG_IMPORT', 'JAVA_TOOL_OPTIONS', 'OP_PROJECT', 'AUTH_JWT_SECRET'])
    assert.throws(() => preflight({[key]: 'nao-exibir'}), /ambiente/);
  assert.doesNotThrow(() => preflight({PATH: 'local'}));
});
test('subrede deve ser IPv4 privado reservado ao plano e nao sobrepor outra rede', () => {
  assert.throws(() => subnetRange('8.8.8.0/24', true), /subrede/);
  assert.throws(() => subnetRange('172.30.256.0/24', true), /subrede/);
  assert.equal(overlaps('172.30.180.0/24', '172.30.0.0/16'), true);
  assert.equal(overlaps('172.30.180.0/24', '172.30.181.0/24'), false);
});
test('recursos de outra execucao nunca sao adotados', () => {
  assert.throws(() => owned({Labels: {'gestaoacoes.operacional.run': 'outra'}}, 'demo'), /propriedade/);
  assert.doesNotThrow(() => owned({Labels: {'gestaoacoes.operacional.run': 'demo'}}, 'demo'));
});
test('Compose aponta somente para arquivo operacional e env externo explicito', () => {
  const args = composeArgs('C:/privado/runtime.env', ['stop', 'frontend', 'backend', 'postgres']);
  assert.equal(args[0], '--context');
  assert.equal(args[1], 'desktop-linux');
  assert.ok(args.includes('--env-file'));
  assert.equal(args.filter(v => v.endsWith('.yaml')).length, 1);
  assert.ok(args.some(v => v.endsWith('compose.operacional.yaml')));
  assert.ok(!args.includes('-v'));
});
test('partida aguarda banco, cria backend antes do proxy e exige bootstrap confirmado', async () => {
  const events = [];
  const io = {compose: (...args) => events.push(args.join(' ')), waitPg: async () => events.push('pg-ready'),
    verifyDatabase: () => events.push('db-identity'), waitReady: async () => events.push('https-ready')};
  await assert.rejects(lifecycle('start', {}, io), /bootstrap/);
  assert.equal(events.length, 0);
  await lifecycle('start', {bootstrapCompleted: true}, io);
  assert.deepEqual(events, ['up -d --no-build --pull never postgres', 'pg-ready', 'db-identity',
    'up -d --no-build --pull never backend', 'up -d --no-build --pull never frontend', 'https-ready']);
});
test('falha de prontidao do banco impede iniciar backend ou frontend', async () => {
  const events = [];
  await assert.rejects(lifecycle('start', {bootstrapCompleted: true}, {
    compose: (...a) => events.push(a.join(' ')), waitPg: async () => {throw Error('pg-falhou');}
  }), /pg-falhou/);
  assert.equal(events.length, 1);
});
test('parada preserva recursos e nao executa down, rm ou revogacao', async () => {
  const events = [];
  await lifecycle('stop', {}, {compose: (...a) => events.push(a)});
  assert.deepEqual(events, [['stop', 'frontend', 'backend', 'postgres']]);
});
test('certificado tem SAN correto e validade que cobre ensaios e entrega', () => {
  const args = certificateArgs('C:/privado');
  assert.equal(args.ca[args.ca.indexOf('-days') + 1], '30');
  assert.equal(args.server[args.server.indexOf('-days') + 1], '30');
  assert.ok(args.verify.includes('gestaoacoes.localhost'));
  assert.ok(!Object.values(args).flat().includes('-k'));
});
test('bases seguem os mesmos digests da infraestrutura homologada', () => {
  const original = fs.readFileSync(path.join(__dirname, 'homologacao.cjs'), 'utf8');
  assert.equal(Object.keys(PINS).length, 3);
  for (const value of Object.values(PINS)) {
    assert.match(value, /@sha256:[a-f0-9]{64}$/);
    assert.ok(original.includes("'" + value + "'"));
  }
});
test('bootstrap recusa ausencia de confirmacao ou repeticao antes de qualquer IO', async () => {
  await assert.rejects(bootstrapAdmin({}, false, {}), /bootstrap/);
  await assert.rejects(bootstrapAdmin({bootstrapCompleted:true}, true, {}), /bootstrap/);
});
test('bootstrap falho nao grava recibo de sucesso e nao inicia servicos web', async () => {
  const state = {bootstrapCompleted:false}; const events = [];
  await assert.rejects(bootstrapAdmin(state,true,{webRunning:()=>false,waitPg:async()=>{},verifyDatabase:()=>{},
    compose:()=>{throw Error('falha-one-shot');},save:()=>events.push('save')}), /falha-one-shot/);
  assert.equal(state.bootstrapCompleted,false); assert.deepEqual(events,[]);
});
test('bootstrap confirmado usa somente o one-shot com remocao do container, preservando volume', async () => {
  const events=[]; const state={bootstrapCompleted:false};
  await bootstrapAdmin(state,true,{webRunning:()=>false,waitPg:async()=>events.push('pg'),
    verifyDatabase:()=>events.push('identity'),compose:(...a)=>events.push(a),save:()=>events.push('receipt')});
  assert.equal(state.bootstrapCompleted,true);
  assert.deepEqual(events,['pg','identity',['--profile','bootstrap','run','--rm','--no-deps','--pull','never','bootstrap'],'receipt']);
});
test('modo deve ser explicito e somente online inclui extensao de apresentacao', () => {
  assert.throws(()=>requireMode(), /modo/);
  assert.throws(()=>requireMode('outro'), /modo/);
  assert.equal(requireMode('sem-integracoes'),'sem-integracoes');
  const online=composeArgs('C:/privado/runtime.env',['config'],'com-integracoes');
  const offline=composeArgs('C:/privado/runtime.env',['config'],'sem-integracoes');
  assert.equal(online.filter(a=>a.endsWith('.yaml')).length,2);
  assert.ok(online.some(a=>a.endsWith('compose.apresentacao-integracoes.yaml')));
  assert.equal(offline.filter(a=>a.endsWith('.yaml')).length,1);
});
function topology(mode) {
  const networks = {api:{},database:{}};
  if(mode==='com-integracoes')networks.egress={gw_priority:1};
  return {services:{
    postgres:{networks:{database:{}},volumes:[{type:'volume',source:'data',target:'/var/lib/postgresql/data'}]},
    bootstrap:{networks:{database:{}}},
    frontend:{networks:{ingress:{},api:{}},ports:[{host_ip:'127.0.0.1',published:'8443',target:8443}]},
    backend:{networks,environment: mode==='com-integracoes' ? {
      BRAPI_BASE_URL:'https://brapi.dev',ALPHA_VANTAGE_BASE_URL:'https://www.alphavantage.co',
      BRASIL_API_BASE_URL:'https://brasilapi.com.br',VIA_CEP_BASE_URL:'https://viacep.com.br'
    } : Object.fromEntries(['BRAPI_BASE_URL','ALPHA_VANTAGE_BASE_URL','BRASIL_API_BASE_URL','VIA_CEP_BASE_URL'].map(k=>[k,'http://127.0.0.1:1'])),
    secrets:mode==='com-integracoes'?[{source:'brapi_key',target:'integration.brapi.api-key'},
      {source:'alpha_key',target:'integration.alpha-vantage.api-key'}]:[]}
  },networks:{api:{external:true},database:{external:true},ingress:{external:true},
    ...(mode==='com-integracoes'?{egress:{external:true}}:{})},volumes:{data:{external:true}}};
}
test('topologias permitem egress somente no backend e offline exige desconexao real', () => {
  for(const mode of ['sem-integracoes','com-integracoes'])assert.doesNotThrow(()=>validateTopology(topology(mode),mode));
  for(const service of ['postgres','frontend','bootstrap']){
    const config=topology('com-integracoes');config.services[service].networks.egress={};
    assert.throws(()=>validateTopology(config,'com-integracoes'),/rede/);
  }
  const config=topology('sem-integracoes');config.services.backend.networks.egress={};
  assert.throws(()=>validateTopology(config,'sem-integracoes'),/rede/);
});
test('topologia recusa portas extras, volume nao persistente ou provider incorreto', () => {
  let config=topology('com-integracoes');config.services.backend.ports=[{target:8080}];
  assert.throws(()=>validateTopology(config,'com-integracoes'),/porta/);
  config=topology('sem-integracoes');config.volumes.data.external=false;
  assert.throws(()=>validateTopology(config,'sem-integracoes'),/volume/);
  config=topology('com-integracoes');config.services.backend.environment.BRAPI_BASE_URL='http://brapi.dev';
  assert.throws(()=>validateTopology(config,'com-integracoes'),/provider/);
});
test('troca de modo recria somente backend, atualiza Nginx e nao chama bootstrap ou volume', async () => {
  const events=[];
  await switchMode('sem-integracoes', {compose:(...args)=>events.push(args),
    verifyAttachments:()=>events.push('networks'),waitReady:async()=>events.push('https')});
  assert.deepEqual(events,[['stop','frontend','backend'],
    ['up','-d','--no-deps','--no-build','--pull','never','--force-recreate','backend'],
    'networks',['up','-d','--no-deps','--no-build','--pull','never','frontend'],
    ['restart','frontend'],'https']);
});
test('troca interrompida na verificacao de rede nao libera frontend', async () => {
  const events=[];
  await assert.rejects(switchMode('sem-integracoes',{compose:(...a)=>events.push(a),
    verifyAttachments:()=>{throw Error('rede-divergente');}}),/rede-divergente/);
  assert.equal(events.length,2);
});
test('inspecao real recusa backend ainda conectado quando modo solicitado e offline', () => {
  const state={id:'run',vars:{OP_DATABASE_NETWORK:'db',OP_API_NETWORK:'api',OP_INGRESS_NETWORK:'in',OP_EGRESS_NETWORK:'out'}};
  const item=networks=>({Config:{Labels:{'gestaoacoes.operacional.run':'run'}},
    NetworkSettings:{Networks:Object.fromEntries(networks.map(n=>[n,{}]))},HostConfig:{PortBindings:{}}});
  assert.doesNotThrow(()=>validateAttachments({backend:item(['api','db'])},state,'sem-integracoes'));
  assert.throws(()=>validateAttachments({backend:item(['api','db','out'])},state,'sem-integracoes'),/rede/);
  assert.doesNotThrow(()=>validateAttachments({backend:item(['api','db','out'])},state,'com-integracoes'));
  for(const service of ['postgres','frontend','bootstrap'])
    assert.throws(()=>validateAttachments({[service]:item(['db','out'])},state,'com-integracoes'),/rede/);
});
test('plano offline novo nao cria egress, e troca reaproveita nomes e volume', () => {
  const state={resources:{},vars:{OP_INGRESS_NETWORK:'in',OP_API_NETWORK:'api',OP_DATABASE_NETWORK:'db',OP_DATA_VOLUME:'data',OP_EGRESS_NETWORK:'out'}};
  const offline=resourcePlan(state,'sem-integracoes');
  const online=resourcePlan(state,'com-integracoes');
  assert.equal(offline.length,4); assert.deepEqual(online,[...offline,['network','out']]);
  state.resources.out='id';
  assert.deepEqual(resourcePlan(state,'sem-integracoes'),online); // verifica rede guardada, nao a associa ao backend
  assert.deepEqual(offline.filter(([kind])=>kind==='volume'),[['volume','data']]);
});
