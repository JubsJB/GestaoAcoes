
'use strict';
const assert = require('node:assert/strict');
const test = require('node:test');
const {frontendTransition,validateFrontendImage} = require('./apresentacao-frontend.cjs');
const A = 'sha256:'+'a'.repeat(64), B = 'sha256:'+'b'.repeat(64), C = 'sha256:'+'c'.repeat(64);

function setup() {
  let receipt = {imageIds:{frontend:A,backend:C,postgres:C}};
  const live = {tag:A,frontend:{image:A,running:true},services:{backend:{id:'backend'},postgres:{id:'postgres'}}};
  const calls = [];
  const io = {
    inspect: () => structuredClone(live),
    validateImage: id => calls.push(['image',id]),
    retain: id => {calls.push(['retain',id]);return id;},
    tag: id => {calls.push(['tag',id]);live.tag=id;},
    reconcile: () => {calls.push(['frontend']);live.frontend={image:live.tag,running:true};},
    ready: () => calls.push(['ready']),
    save: state => {calls.push([state.pendingFrontend?'journal':'receipt']);receipt=structuredClone(state);}
  };
  return {io,live,calls,state:()=>receipt,run:(action,id)=>frontendTransition(action,id,receipt,io)};
}

test('atualiza e restaura por ID, preservando recibos dos serviços protegidos', async () => {
  const f=setup();
  await f.run('update-frontend',B);
  assert.deepEqual(f.calls,[['image',B],['retain',A],['journal'],['image',B],['tag',B],['frontend'],['ready'],['receipt']]);
  assert.deepEqual(f.state(),{imageIds:{frontend:B,backend:C,postgres:C},previousFrontend:A});
  await f.run('rollback-frontend');
  assert.deepEqual(f.state(),{imageIds:{frontend:A,backend:C,postgres:C},previousFrontend:B});
  assert.equal(f.live.frontend.image,A);
});

test('recusa tag mutavel, candidato invalido e drift inicial antes de reconciliar', async () => {
  const f=setup();
  await assert.rejects(f.run('update-frontend','frontend:latest'),/sha256/);
  f.io.validateImage=()=>{throw Error('incompativel');};
  await assert.rejects(f.run('update-frontend',B),/incompativel/);
  f.live.tag=C;
  await assert.rejects(f.run('update-frontend',B),/diverge/);
  assert.deepEqual(f.calls,[]);
});

for (const phase of ['tag','reconcile','ready','receipt']) {
  test('recupera falha em '+phase+' com recibo anterior e sem tentativa duplicada',async()=>{
    const f=setup(), originalSave=f.io.save, originalReconcile=f.io.reconcile, originalTag=f.io.tag, originalReady=f.io.ready;
    if(phase==='tag') f.io.tag=()=>{throw Error('interrupcao');};
    if(phase==='reconcile') f.io.reconcile=()=>{f.live.frontend=null;throw Error('interrupcao');};
    if(phase==='ready') f.io.ready=()=>{throw Error('interrupcao');};
    if(phase==='receipt') f.io.save=state=>{if(!state.pendingFrontend)throw Error('interrupcao');originalSave(state);};
    await assert.rejects(f.run('update-frontend',B),/interrupcao/);
    assert.equal(f.state().imageIds.frontend,A);
    assert.equal(f.state().pendingFrontend.to,B);
    await assert.rejects(f.run('rollback-frontend'),/pendente/);
    Object.assign(f.io,{tag:originalTag,reconcile:originalReconcile,ready:originalReady,save:originalSave});
    const n=f.calls.filter(([call])=>call==='frontend').length;
    await f.run('recover-frontend');
    assert.deepEqual(f.state(),{imageIds:{frontend:A,backend:C,postgres:C}});
    assert.equal(f.live.frontend.image,A);
    assert.equal(f.calls.filter(([call])=>call==='frontend').length,n+(phase==='tag'?0:1));
    await assert.rejects(f.run('recover-frontend'),/Nenhuma/);
  });
}

test('recuperação conclui rollback interrompido e recusa imagem desconhecida ou backend reiniciado',async()=>{
  const f=setup();
  await f.run('update-frontend',B);
  const ready=f.io.ready;
  f.io.ready=()=>{throw Error('interrupcao');};
  await assert.rejects(f.run('rollback-frontend'),/interrupcao/);
  f.io.ready=ready;
  f.live.frontend.image=C;
  await assert.rejects(f.run('recover-frontend'),/desconhecido/);
  f.live.frontend.image=A;
  f.live.services.backend.id='outro';
  await assert.rejects(f.run('recover-frontend'),/Backend/);
  f.live.services.backend.id='backend';
  await f.run('recover-frontend');
  assert.equal(f.state().imageIds.frontend,A);
  assert.equal(f.state().pendingFrontend,undefined);
  assert.equal(f.calls.filter(([call])=>call==='frontend').length,2);
});

test('imagem precisa corresponder a ID, plataforma, rótulo, camadas de base e runtime',()=>{
  const current={Config:{Cmd:['nginx','-g','daemon off;']}};
  const base={RootFS:{Layers:['base-a','base-b']}};
  const image={Id:B,Os:'linux',Architecture:'amd64',RootFS:{Layers:['base-a','base-b','site']},
    Config:{...current.Config,Labels:{'gestaoacoes.operacional.run':'limpo'}}};
  validateFrontendImage(image,B,current,base,'limpo');
  for(const invalid of [{Id:C},{Architecture:'arm64'},{Os:'windows'},{RootFS:{Layers:['outra']}},{Config:{Cmd:['sh']}}, {Config:{...image.Config,User:'root'}}])
    assert.throws(()=>validateFrontendImage({...image,...invalid},B,current,base,'limpo'),/incompativel/);
});


test('escopo operacional recusa projeto original, modo conectado e transição de transporte',()=>{
  const {validateFrontendDeploymentIdentity}=require('./apresentacao.cjs');
  const project='gademofc3279fb5761', id='fc3279fb-5761-4789-ac51-3d67796277f5';
  const state={project,id,imagesBuilt:true,activeMode:'sem-integracoes',activeTransport:'http-local',
    vars:{OP_PROJECT:project,OP_RUN_ID:id,OP_DATA_VOLUME:project+'_data',OP_INGRESS_NETWORK:project+'_ingress',
      OP_API_NETWORK:project+'_api',OP_DATABASE_NETWORK:project+'_database',OP_EGRESS_NETWORK:project+'_egress',
      OP_FRONTEND_IMAGE:project+'-frontend:apresentacao'}};
  validateFrontendDeploymentIdentity(state,'http-local');
  assert.throws(()=>validateFrontendDeploymentIdentity({...state,project:'gademob84f37a3991e'},'http-local'),/exclusivo/);
  assert.throws(()=>validateFrontendDeploymentIdentity({...state,activeMode:'com-integracoes'},'http-local'),/offline/);
  assert.throws(()=>validateFrontendDeploymentIdentity({...state,pendingTransport:'https'},'http-local'),/transicao/);
  assert.throws(()=>validateFrontendDeploymentIdentity({...state,vars:{...state.vars,OP_DATA_VOLUME:'outro'}},'http-local'),/exclusivo/);
});

test('recibo é substituído por inteiro em diretório descartável',t=>{
  const fs=require('node:fs'), path=require('node:path'), os=require('node:os');
  const {saveFrontendReceipt}=require('./apresentacao.cjs');
  const dir=fs.mkdtempSync(path.join(os.tmpdir(),'ga-recibo-teste-'));
  const receipt=path.join(dir,'manifesto.json');
  t.after(()=>{fs.unlinkSync(receipt);fs.rmdirSync(dir);});
  fs.writeFileSync(receipt,JSON.stringify({imageIds:{frontend:A}}));
  saveFrontendReceipt(dir,{imageIds:{frontend:A},pendingFrontend:{from:A,to:B}});
  assert.equal(JSON.parse(fs.readFileSync(receipt,'utf8')).pendingFrontend.to,B);
  saveFrontendReceipt(dir,{imageIds:{frontend:B},previousFrontend:A});
  assert.deepEqual(JSON.parse(fs.readFileSync(receipt,'utf8')),{imageIds:{frontend:B},previousFrontend:A});
  assert.deepEqual(fs.readdirSync(dir),['manifesto.json']);
});
