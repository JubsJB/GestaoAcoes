'use strict';
// Nunca executado ao importar. Sem limpeza automatica, dados de negocio ou bypass TLS.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const https = require('node:https');
const http = require('node:http');
const {spawnSync} = require('node:child_process');
const {frontendActions,frontendTransition,validateFrontendImage,imageId,FrontendTransitionError} = require('./apresentacao-frontend.cjs');
const ROOT = path.resolve(__dirname, '..');
const LABEL = 'gestaoacoes.operacional.run';
const OPENSSL = 'C:/Program Files/Git/usr/bin/openssl.exe';
const PINS = Object.freeze({
  postgres: 'postgres@sha256:eee6c6e1b0894fa4fe0075eb054af9fd1cf39a35321c2d4110626cfe506411fb',
  frontend: 'nginx@sha256:0e52bd054126cdc040b1276c0af347aeb02a41b9cfe305d6094767ac30e97949',
  backend: 'eclipse-temurin@sha256:c35794d1348777e6bc310b9b7f420098ef8f37e988199549b545fded79a895a2'
});
const fail = message => {throw Error(message);};
function externalDirectory(directory, root = ROOT) {
  if (!path.win32.isAbsolute(directory)) fail('Diretorio deve ser absoluto');
  if (/[\r\n=$#"'`]/.test(directory)) fail('Diretorio externo contem caracteres incompativeis');
  const d = path.win32.resolve(directory).toLowerCase();
  const r = path.win32.resolve(root).toLowerCase();
  if (d === r || d.startsWith(r + '\\') || r.startsWith(d.endsWith('\\') ? d : d + '\\'))
    fail('Diretorio privado deve ser externo ao repositorio e nao ser seu ancestral');
  return path.resolve(directory);
}
function preflight(env) {
  for (const key of Object.keys(env))
    if (/^(OP_|APP_|SPRING_|AUTH_|JAVA_TOOL_OPTIONS$|JDK_JAVA_OPTIONS$|_JAVA_OPTIONS$|DOCKER_|COMPOSE_|GA_CADASTRAL_GUARD$)/i.test(key))
      fail('Remova configuracoes de ambiente capazes de redirecionar o runtime');
}
function subnetRange(cidr, planned = false) {
  const match = /^(\d+)\.(\d+)\.(\d+)\.(\d+)\/(\d+)$/.exec(cidr);
  if (!match) fail('subrede IPv4 invalida');
  const [a,b,c,d,bits] = match.slice(1).map(Number);
  if ([a,b,c,d].some(n => n > 255) || bits > 32 ||
      (planned && (a !== 172 || b !== 30 || d !== 0 || bits !== 24))) fail('subrede invalida para o plano');
  const size = 2 ** (32 - bits), base = Math.floor((a*2**24+b*2**16+c*256+d)/size)*size;
  return [base, base + size - 1];
}
function overlaps(a, b) {
  const x = subnetRange(a), y = subnetRange(b);
  return x[0] <= y[1] && y[0] <= x[1];
}
function owned(item, id) {
  if ((item.Labels || item.Config?.Labels || {})[LABEL] !== id) fail('propriedade do recurso nao comprovada');
}
const CADASTRAL_MODE = 'integracoes-cadastrais';
const CADASTRAL_PROJECT = 'gademofc3279fb5761';
const CADASTRAL_ID = 'fc3279fb-5761-4789-ac51-3d67796277f5';
const CADASTRAL_OVERRIDE = 'compose.apresentacao-cadastral.yaml';
const CADASTRAL_HASH = '4b16d5c190480bd2637d542e04395e8da5686ca303431dc37190afe7260f25ca';
const usesEgress = mode => mode === 'com-integracoes' || mode === CADASTRAL_MODE;

function validateCadastralIdentity(state, transport) {
  const v = state.vars;
  if (transport !== 'http-local' || state.id !== CADASTRAL_ID || state.project !== CADASTRAL_PROJECT
      || v.OP_RUN_ID !== CADASTRAL_ID || v.OP_PROJECT !== CADASTRAL_PROJECT
      || Object.entries({DATA_VOLUME:'data', INGRESS_NETWORK:'ingress', API_NETWORK:'api',
        DATABASE_NETWORK:'database', EGRESS_NETWORK:'egress'}).some(([key,suffix]) =>
        v['OP_'+key] !== CADASTRAL_PROJECT+'_'+suffix))
    fail('Modo cadastral exclusivo do projeto limpo e do transporte HTTP local');
}

function requireMode(mode) {
  if (!['sem-integracoes','com-integracoes',CADASTRAL_MODE].includes(mode)) fail('Selecione --modo sem-integracoes ou --modo com-integracoes ou --modo integracoes-cadastrais');
  return mode;
}
const HTTP_OVERRIDE = 'compose.apresentacao-http.yaml';
const HTTP_NGINX = 'ops/nginx.apresentacao-http.conf';
const HTTP_PORT = '8081';
function requireTransport(transport = 'https') {
  if (!['https','http-local'].includes(transport)) fail('Selecione --transporte https ou http-local');
  return transport;
}
function transportPort(transport) {
  return requireTransport(transport) === 'http-local' ? HTTP_PORT : '8443';
}
function composeArgs(envfile, args, mode = 'sem-integracoes', transport = 'https') {
  requireMode(mode);
  return ['--context', 'desktop-linux', 'compose', '--env-file', envfile,
    '-f', path.join(ROOT, 'compose.operacional.yaml'),
    ...(mode === 'com-integracoes' ? ['-f',path.join(ROOT,'compose.apresentacao-integracoes.yaml')] : []),
    ...(mode === CADASTRAL_MODE ? ['-f',path.join(ROOT,CADASTRAL_OVERRIDE)] : []),
    ...(requireTransport(transport) === 'http-local' ? ['-f',path.join(ROOT,HTTP_OVERRIDE)] : []), ...args];
}
const PROVIDERS = Object.freeze({BRAPI_BASE_URL:'https://brapi.dev',
  ALPHA_VANTAGE_BASE_URL:'https://www.alphavantage.co', BRASIL_API_BASE_URL:'https://brasilapi.com.br',
  VIA_CEP_BASE_URL:'https://viacep.com.br'});
function validateTopology(config, mode, transport = 'https') {
  requireMode(mode);
  const s = config.services;
  const same = (a,b) => JSON.stringify([...a].sort()) === JSON.stringify([...b].sort());
  const expected = {postgres:['database'],bootstrap:['database'],frontend:['ingress','api'],
    backend:['api','database',...(usesEgress(mode) ? ['egress'] : [])]};
  if (!same(Object.keys(s),Object.keys(expected))) fail('Servicos inesperados na topologia');
  for (const [service,networks] of Object.entries(expected)) {
    if (!same(Object.keys(s[service].networks || {}),networks)) fail('Associacao de rede incompativel');
    if (service !== 'frontend' && (s[service].ports || []).length) fail('porta publicada indevidamente');
  }
  const ports = s.frontend.ports || [];
  const port = transportPort(transport);
  if (ports.length !== 1 || ports[0].host_ip !== '127.0.0.1' || String(ports[0].published) !== port
      || Number(ports[0].target) !== Number(port) || (ports[0].protocol || 'tcp') !== 'tcp')
    fail('porta web deve ser exclusivamente loopback');
  if (transport === 'http-local') {
    const mounts = s.frontend.volumes || [];
    const mount = mounts[0];
    if (mounts.length !== 1 || mount.type !== 'bind' || !mount.read_only
        || mount.target !== '/etc/nginx/conf.d/default.conf'
        || typeof mount.source !== 'string' || !mount.source.replaceAll('\\','/').endsWith('/http-local/nginx.conf')
        || !s.backend.environment.APP_TRUSTED_PROXY_IP
        || s.frontend.networks.api.ipv4_address !== s.backend.environment.APP_TRUSTED_PROXY_IP
        || (s.frontend.secrets || []).length
        || s.backend.environment.SPRING_PROFILES_ACTIVE !== 'operacional,apresentacao-http'
        || s.backend.environment.APP_PUBLIC_ORIGIN !== 'http://localhost:8081')
      fail('configuracao de ingresso HTTP divergente');
  }
  if (!config.volumes?.data?.external || !(s.postgres.volumes || []).some(v=>v.type === 'volume' && v.source === 'data' && v.target === '/var/lib/postgresql/data'))
    fail('volume persistente externo obrigatorio');
  for (const [key,url] of Object.entries(PROVIDERS)) {
    const enabled = mode === 'com-integracoes' ||
      (mode === CADASTRAL_MODE && ['BRASIL_API_BASE_URL','VIA_CEP_BASE_URL'].includes(key));
    if (s.backend.environment[key] !== (enabled ? url : 'http://127.0.0.1:1')) fail('Destino de provider incompativel');
  }
  if (mode === CADASTRAL_MODE) {
    const labels = s.backend.labels || {};
    if (transport !== 'http-local' || config.name !== CADASTRAL_PROJECT
        || config.volumes.data.name !== CADASTRAL_PROJECT+'_data'
        || labels['gestaoacoes.apresentacao.cadastral'] !== CADASTRAL_ID
        || Object.values(s).some(service => service.labels?.[LABEL] !== CADASTRAL_ID)
        || ['ingress','api','database','egress'].some(key => config.networks?.[key]?.name !== CADASTRAL_PROJECT+'_'+key))
      fail('Identidade Compose do modo cadastral divergente');
    const expectedSecrets = ['app_password:spring.datasource.password','jwt:auth.jwt.secret','reset:auth.reset.secret'];
    if (!same((s.backend.secrets || []).map(secret => secret.source+':'+secret.target),expectedSecrets)
        || ['brapi_key','alpha_key'].some(key => Object.hasOwn(config.secrets || {},key)))
      fail('Segredos inesperados na configuracao cadastral');
  }
  if (usesEgress(mode)) {
    if (!config.networks?.egress?.external || s.backend.networks.egress.gw_priority !== 1) fail('rede de saida com gateway explicito obrigatoria');
  }
  if (mode === 'com-integracoes') {
    for (const [source,target] of [['brapi_key','integration.brapi.api-key'],['alpha_key','integration.alpha-vantage.api-key']])
      if (!(s.backend.secrets || []).some(v=>v.source === source && v.target === target)) fail('Segredo externo de provider ausente');
  } else if ((s.backend.secrets || []).some(v=>['brapi_key','alpha_key'].includes(v.source))) fail('Segredos de providers presentes no modo sem integracoes');
}
function providerFiles(directory) {
  for (const name of ['brapi-api-key','alpha-vantage-api-key']) {
    const file=path.join(directory,name);
    if (!fs.existsSync(file) || !/^\S+$/.test(fs.readFileSync(file,'utf8').trim())) fail('Configurar chaves de providers nos arquivos privados; valores nao publicados');
  }
}
async function switchMode(mode, io) {
  requireMode(mode);
  io.compose('stop','frontend','backend');
  io.compose('up','-d','--no-deps','--no-build','--pull','never','--force-recreate','backend');
  io.verifyAttachments();
  io.compose('up','-d','--no-deps','--no-build','--pull','never','frontend');
  // Backend recriado pode receber outro IP; atualizar a resolucao do upstream Nginx.
  io.compose('restart','frontend');
  await io.waitReady();
}
function certificateArgs(directory) {
  const p = name => path.join(directory, name);
  return {
    ca: ['req','-x509','-newkey','rsa:3072','-nodes','-days','30','-subj','/CN=GestaoAcoes Demo Local CA',
      '-addext','basicConstraints=critical,CA:TRUE','-addext','keyUsage=critical,keyCertSign,cRLSign',
      '-keyout',p('ca.key'),'-out',p('ca.crt')],
    request: ['req','-newkey','rsa:3072','-nodes','-subj','/CN=gestaoacoes.localhost',
      '-keyout',p('server.key'),'-out',p('server.csr')],
    server: ['x509','-req','-days','30','-in',p('server.csr'),'-CA',p('ca.crt'),'-CAkey',p('ca.key'),
      '-CAcreateserial','-extfile',p('extensions.cnf'),'-out',p('server.crt')],
    verify: ['verify','-CAfile',p('ca.crt'),'-verify_hostname','gestaoacoes.localhost',p('server.crt')]
  };
}
async function lifecycle(action, state, io) {
  if (action === 'stop') return io.compose('stop', 'frontend', 'backend', 'postgres');
  if (action === 'start' && !state.bootstrapCompleted) fail('Confirme o bootstrap inicial antes da partida web');
  if (io.preserveDatabase) io.compose('start','postgres');
  else io.compose('up','-d','--no-build','--pull','never','postgres');
  await io.waitPg();
  io.verifyDatabase();
  if (action === 'start-db') return;
  if (io.switching) return switchMode(io.mode, io);
  io.compose('up','-d','--no-build','--pull','never','backend');
  io.verifyAttachments?.();
  io.compose('up','-d','--no-build','--pull','never','frontend');
  await io.waitReady();
}
async function bootstrapAdmin(state, confirmed, io) {
  if (!confirmed || state.bootstrapCompleted) fail('bootstrap exige --primeiro-admin e nunca se repete automaticamente');
  if (io.webRunning()) fail('Parar servicos web antes do bootstrap');
  await io.waitPg(); io.verifyDatabase();
  io.compose('--profile','bootstrap','run','--rm','--no-deps','--pull','never','bootstrap');
  state.bootstrapCompleted = true; io.save();
}
function command(exe, args, extraEnv = {}) {
  const result = spawnSync(exe, args, {cwd: ROOT, encoding:'utf8', windowsHide:true,
    timeout: 240000, maxBuffer: 16*1024*1024, env:{...process.env, ...extraEnv, MSYS_NO_PATHCONV:'1'}});
  // Nao imprimir stdout/stderr/argv de subprocessos: podem conter material sensivel.
  if (result.status !== 0 || result.error) fail('Comando operacional falhou; verificar etapa no runbook (saida nao publicada)');
  return result.stdout.trim();
}
const docker = (...args) => command('docker', ['--context','desktop-linux', ...args]);
const inspect = (kind, name) => JSON.parse(docker(kind, 'inspect', name))[0];
const digest = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const configFiles = ['compose.operacional.yaml','compose.apresentacao-integracoes.yaml','ops/nginx.conf','ops/postgres-init.sh',
  'ops/backend.Dockerfile','ops/frontend.Dockerfile','ops/postgres.Dockerfile',
  'src/main/resources/application-operacional.properties'];
function save(directory, state) {
  fs.writeFileSync(path.join(directory, 'manifesto.json'), JSON.stringify(state, null, 2) + '\n');
}
function prepare(directory, subnet) {
  subnetRange(subnet, true);
  if (fs.existsSync(directory)) fail('Use diretorio novo; nunca sobrescrever chaves ou manifesto existentes');
  fs.mkdirSync(directory, {recursive:true});
  const sid = command('whoami.exe', ['/user','/fo','csv','/nh']).match(/S-1-5-[\d-]+/);
  if (!sid) fail('Identidade Windows nao identificada');
  command('icacls.exe', [directory, '/inheritance:r','/grant:r',`*${sid[0]}:(OI)(CI)F`,'*S-1-5-18:(OI)(CI)F']);
  const id = crypto.randomUUID(), project = 'gademo' + id.replaceAll('-','').slice(0,12);
  const state = {version:2, id, project, subnet, bootstrapCompleted:false, imagesBuilt:false, resources:{}, activeMode:null,
    hashes:Object.fromEntries(configFiles.map(f => [f,digest(path.join(ROOT,f))]))};
  const vars = {OP_PROJECT: project, OP_RUN_ID:id, OP_PRIVATE_DIR:directory.replaceAll('\\','/'),
    OP_DB_NAME:'demonstracao', OP_DB_APP_USER:'aplicacao', OP_BOOTSTRAP_USER:'admin_demo',
    OP_INGRESS_NETWORK:project+'_ingress', OP_API_NETWORK:project+'_api', OP_DATABASE_NETWORK:project+'_database',
    OP_DATA_VOLUME:project+'_data', OP_EGRESS_NETWORK:project+'_egress', APP_TRUSTED_PROXY_IP:subnet.replace('0/24','10')};
  for (const service of Object.keys(PINS)) vars['OP_'+service.toUpperCase()+'_IMAGE'] = project+'-'+service+':apresentacao';
  state.vars = vars;
  for (const name of ['auth.jwt.secret','auth.reset.secret','app-password','provision-password','AUTH_BOOTSTRAP_PASSWORD'])
    fs.writeFileSync(path.join(directory,name), crypto.randomBytes(48).toString('base64'), {flag:'wx'});
  fs.writeFileSync(path.join(directory,'extensions.cnf'), 'subjectAltName=DNS:gestaoacoes.localhost\nbasicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\n');
  const args = certificateArgs(directory);
  for (const stage of ['ca','request','server','verify']) command(OPENSSL,args[stage]);
  fs.writeFileSync(path.join(directory,'runtime.env'), Object.entries(vars).map(([k,v])=>k+'='+v).join('\n')+'\n', {flag:'wx'});
  state.envHash = digest(path.join(directory,'runtime.env'));
  save(directory,state);
}
function validateFiles(directory, state) {
  if (state.version !== 2 || !/^gademo[a-f0-9]{12}$/.test(state.project) ||
      state.vars.OP_PRIVATE_DIR !== directory.replaceAll('\\','/')) fail('Manifesto incompativel');
  if (digest(path.join(directory,'runtime.env')) !== state.envHash) fail('runtime.env alterado; revisar antes de executar');
  for (const [file, expected] of Object.entries(state.hashes))
    if (digest(path.join(ROOT,file)) !== expected) fail('Configuracao operacional mudou; revisar manifesto antes de executar');
}
function validateFrontendBinding(bindings, transport) {
  const port = transportPort(transport);
  const listeners = bindings?.[port+'/tcp'];
  if (Object.keys(bindings || {}).length !== 1 || listeners?.length !== 1 ||
      listeners[0].HostIp !== '127.0.0.1' || listeners[0].HostPort !== port)
    fail('publicacao efetiva deve ser exclusivamente loopback');
}
function validateAttachments(containers, state, mode, transport) {
  requireMode(mode);
  const v=state.vars;
  const expected={postgres:[v.OP_DATABASE_NETWORK],frontend:[v.OP_INGRESS_NETWORK,v.OP_API_NETWORK],
    bootstrap:[v.OP_DATABASE_NETWORK],backend:[v.OP_API_NETWORK,v.OP_DATABASE_NETWORK,
      ...(usesEgress(mode)?[v.OP_EGRESS_NETWORK]:[])]};
  for(const [service,item] of Object.entries(containers)) {
    owned(item,state.id);
    if (!expected[service] || JSON.stringify(Object.keys(item.NetworkSettings.Networks).sort()) !== JSON.stringify(expected[service].sort()))
      fail('Associacao real de rede diverge do modo selecionado');
    if (service === 'backend' && mode === CADASTRAL_MODE &&
        (item.Config.Labels['gestaoacoes.apresentacao.cadastral'] !== CADASTRAL_ID ||
          (item.Mounts || []).some(mount =>
            [mount.Source,mount.Destination].some(location =>
              /(?:brapi|alpha-vantage)[.-]api-key$/i.test(location || '')))))
      fail('Marcador ou montagens reais do backend cadastral divergentes');
    if (service === 'frontend' && transport && item.State.Running) {
      validateFrontendBinding(item.HostConfig.PortBindings, transport);
      if (item.NetworkSettings.Networks[v.OP_API_NETWORK].IPAddress !== v.APP_TRUSTED_PROXY_IP)
        fail('Endereco real do Nginx diverge do proxy confiavel');
    }
    if (service !== 'frontend' && Object.values(item.HostConfig.PortBindings || {}).some(bindings=>bindings?.length)) fail('porta real publicada indevidamente');
  }
}
function resourcePlan(state, mode) {
  return [['network',state.vars.OP_INGRESS_NETWORK],['network',state.vars.OP_API_NETWORK],
    ['network',state.vars.OP_DATABASE_NETWORK],['volume',state.vars.OP_DATA_VOLUME],
    ...(usesEgress(mode) || state.resources[state.vars.OP_EGRESS_NETWORK] ? [['network',state.vars.OP_EGRESS_NETWORK]] : [])];
}
function verifyResources(state, mode = 'sem-integracoes') {
  const v = state.vars;
  for (const [kind,name] of resourcePlan(state,mode)) {
    const item = inspect(kind,name); owned(item,state.id);
    if (state.resources[name] !== (item.Id || item.Name)) fail('Identidade do recurso mudou');
    if (kind === 'network') {
      const internal = [v.OP_API_NETWORK,v.OP_DATABASE_NETWORK].includes(name);
      if (item.Internal !== internal) fail('Isolamento de rede incompativel');
      if (name === v.OP_API_NETWORK && !item.IPAM.Config.some(c => c.Subnet === state.subnet)) fail('Subrede API divergente');
      const allowed = name === v.OP_EGRESS_NETWORK ? ['backend'] : name === v.OP_INGRESS_NETWORK ? ['frontend'] :
        name === v.OP_API_NETWORK ? ['backend','frontend'] : ['postgres','backend','bootstrap'];
      for (const cid of Object.keys(item.Containers || {})) {
        const peer=inspect('container',cid); owned(peer,state.id);
        if (!allowed.includes(peer.Config.Labels['com.docker.compose.service'])) fail('Membro de rede incompativel');
      }
    }
  }
}
function verifyImages(state) {
  if (!state.imagesBuilt) fail('Prepare imagens primeiro');
  for (const service of Object.keys(PINS)) {
    const image = inspect('image',state.vars['OP_'+service.toUpperCase()+'_IMAGE']); owned(image,state.id);
    if (state.imageIds[service] !== image.Id) fail('Imagem mudou desde a preparacao');
  }
}
function resources(directory,state,mode) {
  const ids = docker('network','ls','-q').split(/\s+/).filter(Boolean);
  for (const id of ids) {
    const item = inspect('network',id);
    if (item.Name === state.vars.OP_API_NETWORK) continue;
    for (const config of item.IPAM.Config || []) if (config.Subnet && !config.Subnet.includes(':') && overlaps(state.subnet,config.Subnet))
      fail('Subrede API sobrepoe rede Docker existente; escolher novo plano antes de continuar');
  }
  for (const [type,name] of resourcePlan(state,mode)) {
    const names = docker(type,'ls','--format','{{.Name}}').split(/\r?\n/);
    if (names.includes(name)) {
      const item = inspect(type,name); owned(item,state.id);
      if (state.resources[name] !== (item.Id || item.Name)) fail('Recurso existente sem recibo; nao adotar automaticamente');
      continue;
    }
    if (state.resources[name]) fail('Recurso registrado ausente; nao recriar volume ou rede automaticamente');
    const args = [type,'create','--label',LABEL+'='+state.id];
    if (name === state.vars.OP_API_NETWORK) args.push('--internal','--subnet',state.subnet);
    if (name === state.vars.OP_DATABASE_NETWORK) args.push('--internal');
    args.push(name); docker(...args);
    const item = inspect(type,name); owned(item,state.id);
    state.resources[name] = item.Id || item.Name; save(directory,state);
  }
  verifyResources(state,mode);
}
function build(directory,state) {
  if (state.imagesBuilt) fail('Imagens ja preparadas; nao substituir imagens da apresentacao automaticamente');
  state.imageIds ||= {};
  for (const service of Object.keys(PINS)) {
    const base = inspect('image',PINS[service]);
    if (base.Architecture !== 'amd64') fail('Base linux/amd64 obrigatoria');
    const dir = path.join(directory,'build-'+service);
    if (fs.existsSync(dir)) fail('Contexto de build existente; revisar preparacao parcial sem sobrescrever');
    fs.mkdirSync(dir);
    fs.copyFileSync(path.join(ROOT,'ops',service+'.Dockerfile'),path.join(dir,'Dockerfile'));
    if (service === 'backend') fs.copyFileSync(path.join(ROOT,'target/gestaoacoes-0.0.1-SNAPSHOT.jar'),path.join(dir,'app.jar'));
    if (service === 'frontend') {
      fs.cpSync(path.join(ROOT,'frontend/dist/frontend/browser'),path.join(dir,'browser'),{recursive:true});
      fs.copyFileSync(path.join(ROOT,'ops/nginx.conf'),path.join(dir,'nginx.conf'));
    }
    if (service === 'postgres') fs.copyFileSync(path.join(ROOT,'ops/postgres-init.sh'),path.join(dir,'postgres-init.sh'));
    const tag = state.vars['OP_'+service.toUpperCase()+'_IMAGE'];
    docker('build','--platform','linux/amd64','--label',LABEL+'='+state.id,'--build-arg','RUNTIME_IMAGE='+PINS[service],'-t',tag,dir);
    state.imageIds[service] = inspect('image',tag).Id; save(directory,state);
  }
  state.imagesBuilt = true; save(directory,state);
}
function verifyHttpReceipt(expected, actual) {
  if (!expected || ['imageId','configHash','overrideHash','dockerfileHash'].some(key => expected[key] !== actual[key]))
    fail('Artefato HTTP sem recibo valido ou alterado desde a preparacao');
}
function httpArtifacts(directory, state, prepareImage = false) {
  const dir = path.join(directory,'http-local');
  const receipt = path.join(dir,'recibo.json');
  const nginx = path.join(dir,'nginx.conf');
  const tag = state.project+'-backend:http-local';
  if (prepareImage) {
    if (!state.bootstrapCompleted) fail('A preparacao HTTP exige apresentacao ja inicializada');
    if (fs.existsSync(dir) || docker('image','ls','--quiet','--filter','reference='+tag))
      fail('Artefatos HTTP ja existem ou preparacao parcial exige revisao');
    if (inspect('image',PINS.backend).Architecture !== 'amd64') fail('Base linux/amd64 obrigatoria');
    const jar = path.join(ROOT,'target/gestaoacoes-0.0.1-SNAPSHOT.jar');
    if (!fs.existsSync(jar)) fail('Empacote o JAR da apresentacao antes de preparar a imagem HTTP');
    fs.mkdirSync(dir);
    fs.copyFileSync(path.join(ROOT,HTTP_NGINX),nginx);
    const context = path.join(dir,'build');
    fs.mkdirSync(context);
    fs.copyFileSync(jar,path.join(context,'app.jar'));
    fs.copyFileSync(path.join(ROOT,'ops/backend.Dockerfile'),path.join(context,'Dockerfile'));
    docker('build','--platform','linux/amd64','--label',LABEL+'='+state.id,
      '--build-arg','RUNTIME_IMAGE='+PINS.backend,'-t',tag,context);
  }
  const image = inspect('image',tag);
  owned(image,state.id);
  const actual = {imageId:image.Id, configHash:digest(nginx), overrideHash:digest(path.join(ROOT,HTTP_OVERRIDE)),
    dockerfileHash:digest(path.join(ROOT,'ops/backend.Dockerfile'))};
  if (actual.configHash !== digest(path.join(ROOT,HTTP_NGINX))) fail('Configuracao Nginx HTTP diverge da fonte');
  if (prepareImage) fs.writeFileSync(receipt,JSON.stringify(actual),{flag:'wx'});
  else verifyHttpReceipt(JSON.parse(fs.readFileSync(receipt,'utf8')), actual);
  return actual.imageId;
}
function requestReady(ca, route = '/api/auth/csrf', status = 204) {
  return new Promise(resolve => {
    const client = ca ? https : http;
    const options = ca ? {hostname:'gestaoacoes.localhost',port:8443,family:4,ca}
      : {hostname:'127.0.0.1',port:HTTP_PORT,headers:{Host:'localhost:'+HTTP_PORT}};
    const req = client.get({...options,path:route,timeout:3000},
      response => {response.resume(); resolve(response.statusCode === status);});
    req.on('timeout',()=>req.destroy()); req.on('error',()=>resolve(false));
  });
}
async function main(argv = process.argv.slice(2)) {
  const [action, rawDirectory, option] = argv;
  if (action === 'help' || !action) {
    console.log('Frontend: update-frontend DIRETORIO sha256:ID | rollback-frontend DIRETORIO | recover-frontend DIRETORIO');
    console.log('Uso: node ops/apresentacao.cjs prepare|build|build-http|resources|check|start-db|bootstrap|start|stop|status DIRETORIO [SUBREDE|--primeiro-admin|--autorizar-migrations] [--modo sem-integracoes|com-integracoes|integracoes-cadastrais] [--transporte https|http-local]. resources/check/start exigem modo.'); return;
  }
  if (!['prepare','build','build-http','resources','check','start-db','bootstrap','start','stop','status',...frontendActions].includes(action)) fail('Acao desconhecida');
  const transportIndex = argv.indexOf('--transporte');
  if (argv.filter(arg=>arg==='--transporte').length > 1 ||
      (transportIndex >= 0 && (!argv[transportIndex+1] || !['check','start'].includes(action))))
    fail('Selecione um transporte apenas em check/start');
  const frontendAction = frontendActions.includes(action);
  if (frontendAction && (argv.length !== (action === 'update-frontend' ? 3 : 2)
      || (action === 'update-frontend' && !imageId.test(option)))) fail('Argumentos de frontend invalidos');
  const transport = frontendAction ? 'http-local' : requireTransport(transportIndex < 0 ? undefined : argv[transportIndex+1]);
  const modeIndex=argv.indexOf('--modo');
  if (argv.filter(v=>v==='--modo').length>1) fail('Selecione um unico modo');
  const mode=['resources','check','start'].includes(action) ? requireMode(modeIndex<0?undefined:argv[modeIndex+1]) : 'sem-integracoes';
  if(modeIndex>=0 && !['resources','check','start'].includes(action)) fail('modo so se aplica a resources/check/start');
  if (process.platform !== 'win32') fail('Roteiro especifico para Windows');
  preflight(process.env);
  let directory = externalDirectory(rawDirectory || '');
  // Resolver junctions antes de criar arquivos privados, inclusive em diretorios ainda inexistentes.
  const suffix = []; let ancestor = directory;
  while (!fs.existsSync(ancestor)) {suffix.unshift(path.basename(ancestor)); ancestor = path.dirname(ancestor);}
  directory = externalDirectory(path.join(fs.realpathSync(ancestor),...suffix), fs.realpathSync(ROOT));
  if (action === 'prepare') {prepare(directory, option); console.log('Materiais privados preparados; nenhum container iniciado.'); return;}
  const lock = path.join(directory,'execucao.lock');
  if (fs.existsSync(lock)) fail('Execucao concorrente ou interrompida; revisar antes de prosseguir');
  const lockFd = fs.openSync(lock,'wx');
  try {
    await execute(action, option, argv, directory, mode, transport);
  } finally {
    fs.closeSync(lockFd);
    fs.unlinkSync(lock);
  }
}

function validateFrontendDeploymentIdentity(state, transport) {
  validateCadastralIdentity(state,transport);
  if (state.activeMode !== 'sem-integracoes' || state.activeTransport !== 'http-local'
      || state.pendingMode || state.pendingTransport || !state.imagesBuilt
      || state.vars.OP_FRONTEND_IMAGE !== CADASTRAL_PROJECT+'-frontend:apresentacao')
    fail('Atualizacao de frontend exige apresentacao limpa offline sem transicao de modo');
}

function saveFrontendReceipt(directory, state) {
  const destination = path.join(directory,'manifesto.json');
  const temporary = path.join(directory,'manifesto-frontend-'+crypto.randomUUID()+'.tmp');
  try {
    fs.writeFileSync(temporary,JSON.stringify(state,null,2)+'\n',{flag:'wx',flush:true});
    fs.renameSync(temporary,destination);
  } finally {
    if (fs.existsSync(temporary)) fs.unlinkSync(temporary);
  }
}

async function deployFrontend(action, option, directory, state, config, compose, container, httpImageId) {
  const v = state.vars;
  const tag = v.OP_FRONTEND_IMAGE;
  if (config.name !== state.project || config.volumes.data.name !== v.OP_DATA_VOLUME
      || config.services.frontend.image !== tag || config.services.postgres.image !== v.OP_POSTGRES_IMAGE
      || config.services.backend.image !== state.project+'-backend:http-local'
      || Object.values(config.services).some(service => service.labels?.[LABEL] !== state.id)
      || Object.entries({ingress:v.OP_INGRESS_NETWORK,api:v.OP_API_NETWORK,database:v.OP_DATABASE_NETWORK})
        .some(([key,name]) => config.networks[key]?.name !== name))
    fail('Identidade Compose de frontend divergente');

  verifyResources(state);
  for (const service of ['postgres','backend']) {
    const image = inspect('image',v['OP_'+service.toUpperCase()+'_IMAGE']);
    owned(image,state.id);
    if (state.imageIds[service] !== image.Id) fail('Imagem de servico protegido divergente');
  }
  const marker = compose('exec','-T','-e','PGOPTIONS=-c default_transaction_read_only=on','postgres',
    'psql','-X','-At','-v','ON_ERROR_STOP=1','-U','provisionador','-d',v.OP_DB_NAME,'-c','SELECT id FROM ops_guard.execution');
  if (marker !== state.id) fail('Banco nao pertence a esta apresentacao');

  const base = inspect('image',PINS.frontend);
  const describe = item => ({id:item.Id,image:item.Image,startedAt:item.State.StartedAt});
  const io = {
    inspect: () => {
      const postgres = container('postgres'), backend = container('backend');
      if (!postgres.State.Running || postgres.State.Health?.Status !== 'healthy'
          || postgres.Image !== state.imageIds.postgres || !postgres.Mounts.some(mount =>
            mount.Type === 'volume' && mount.Name === v.OP_DATA_VOLUME && mount.Destination === '/var/lib/postgresql/data')
          || !backend.State.Running || backend.Image !== httpImageId
          || (backend.Mounts || []).some(mount => /brapi|alpha-vantage/i.test(mount.Destination)))
        fail('Servicos protegidos fora do estado aprovado');
      const id = compose('ps','-a','-q','frontend');
      if (/\s/.test(id)) fail('Container frontend unico esperado');
      const item = id ? inspect('container',id) : null;
      const containers = {postgres,backend,...(item ? {frontend:item} : {})};
      for (const [service,value] of Object.entries(containers))
        if (value.Config.Labels['com.docker.compose.project'] !== state.project
            || value.Config.Labels['com.docker.compose.service'] !== service)
          fail('Identidade de container Compose divergente');
      validateAttachments(containers,state,'sem-integracoes','http-local');
      const tagged = inspect('image',tag);
      owned(tagged,state.id);
      return {tag:tagged.Id,frontend:item && {image:item.Image,running:item.State.Running},
        services:{backend:describe(backend),postgres:describe(postgres)}};
    },
    validateImage: (id,current) => validateFrontendImage(inspect('image',id),id,inspect('image',current),base,state.id),
    retain: id => {
      const retained = state.project+'-frontend:rollback-'+id.slice(7);
      if (!docker('image','ls','--quiet','--filter','reference='+retained))
        docker('image','tag',id,retained);
      return inspect('image',retained).Id;
    },
    tag: id => docker('image','tag',id,tag),
    reconcile: () => compose('up','-d','--no-deps','--no-build','--pull','never','--force-recreate','frontend'),
    ready: async () => {
      for (let n=0;n<30;n++) {
        if (await requestReady(null,'/',200) && await requestReady(null)) return;
        await sleep(1000);
      }
      fail('Nginx ou API indisponivel; recupere a transicao de frontend');
    },
    save: receipt => saveFrontendReceipt(directory,receipt)
  };
  try {
    await frontendTransition(action,option,state,io);
  } catch (error) {
    if (error instanceof FrontendTransitionError) fail(error.message);
    throw error;
  }
}

async function execute(action, option, argv, directory, mode, transport) {
  const state = JSON.parse(fs.readFileSync(path.join(directory,'manifesto.json'),'utf8'));
  validateFiles(directory,state);
  const frontendAction = frontendActions.includes(action);
  if (state.pendingFrontend && !frontendAction) fail('Transicao de frontend pendente; use recover-frontend');
  if (frontendAction) validateFrontendDeploymentIdentity(state,transport);
  if (mode === CADASTRAL_MODE) {
    if (action === 'resources') transport = 'http-local';
    validateCadastralIdentity(state,transport);
    const contents = fs.readFileSync(path.join(ROOT,CADASTRAL_OVERRIDE),'utf8').replaceAll('\r\n','\n');
    if (crypto.createHash('sha256').update(contents).digest('hex') !== CADASTRAL_HASH) fail('Override cadastral divergente');
  }
  if (docker('context','inspect','desktop-linux','--format','{{.Endpoints.docker.Host}}') !== 'npipe:////./pipe/dockerDesktopLinuxEngine')
    fail('Endpoint Docker local divergente do roteiro homologado');
  if (docker('version','--format','{{.Server.Os}}/{{.Server.Arch}}') !== 'linux/amd64') fail('Docker Linux amd64 obrigatorio');
  if (transport === 'http-local' || action === 'build-http') {
    const version = docker('version','--format','{{.Server.Version}}');
    if (!(Number(/^(\d+)\./.exec(version)?.[1]) >= 28)) fail('HTTP local exige Docker Engine 28 ou superior');
  }
  const compose = (...args) => command('docker',composeArgs(path.join(directory,'runtime.env'),args,mode,transport),
    mode === CADASTRAL_MODE ? {GA_CADASTRAL_GUARD:CADASTRAL_ID} : {});
  const baseCompose = (...args) => command('docker',composeArgs(path.join(directory,'runtime.env'),args,'sem-integracoes'));
  const container = service => {
    const id = baseCompose('ps','-a','-q',service);
    if (!id || /\s/.test(id)) fail('Container unico esperado');
    const item = inspect('container',id); owned(item,state.id); return item;
  };
  if (action === 'build') {build(directory,state); return;}
  if (action === 'build-http') {
    verifyResources(state);
    verifyImages(state);
    httpArtifacts(directory,state,true);
    console.log('Imagem HTTP adicional registrada; containers e dados preservados.');
    return;
  }
  const httpImageId = transport === 'http-local' ? httpArtifacts(directory,state) : null;
  if (mode === 'com-integracoes') providerFiles(directory);
  // Renderizacao real do Compose, sem imprimir configuracao ou depender de readiness.
  // Exige uma versao Compose com suporte a gw_priority no modo conectado.
  const config = JSON.parse(compose('--profile','bootstrap','config','--format','json'));
  validateTopology(config,mode,transport);
  if (frontendAction) {
    await deployFrontend(action,option,directory,state,config,compose,container,httpImageId);
    console.log('Frontend conferido e recibo atualizado; execute check e confira o navegador.');
    return;
  }
  if (action === 'resources') {
    // Nesta janela, somente a rede de saida pode ser provisionada.
    if (mode === CADASTRAL_MODE) verifyResources(state);
    resources(directory,state,mode);
    return;
  }
  verifyResources(state,mode); verifyImages(state);
  for (const service of ['postgres','backend','frontend','bootstrap']) {
    const id = baseCompose('ps','-a','-q',service);
    if (id) owned(inspect('container',id),state.id);
  }
  const verifyAttachments = () => {
    const containers={};
    for(const service of ['postgres','backend','frontend','bootstrap']) {
      const id=baseCompose('ps','-a','-q',service);
      if(id)containers[service]=container(service);
    }
    validateAttachments(containers,state,mode,transport);
    for (const service of ['backend','frontend']) {
      const item = containers[service];
      const expected = service === 'backend' && httpImageId ? httpImageId : state.imageIds[service];
      if (item?.State.Running && item.Image !== expected) fail('Imagem web em execucao diverge do recibo');
    }
  };
  if (action === 'check') {verifyAttachments();console.log('Topologia '+mode+'/'+transport+' coerente; navegador e conectividade exigem ensaio integrado.'); return;}
  if (action === 'status') {
    console.log('Ultimo modo confirmado: '+(state.activeMode || 'nenhum')+'; transicao pendente: '+(state.pendingMode || 'nenhuma'));
    console.log('Transporte confirmado: '+(state.activeTransport || 'https')+'; pendente: '+(state.pendingTransport || 'nenhum'));
    for (const service of ['postgres','backend','frontend']) {
      const item = container(service);
      console.log(service+': '+item.State.Status+' / '+(item.State.Health?.Status || 'sem healthcheck'));
    }
    return;
  }
  const preserveDatabase = action === 'start' &&
    [transport,state.activeTransport,state.pendingTransport].includes('http-local');
  if (preserveDatabase) {
    const postgres = container('postgres');
    if (postgres.Image !== state.imageIds.postgres || !postgres.Mounts.some(mount =>
      mount.Type === 'volume' && mount.Name === state.vars.OP_DATA_VOLUME &&
      mount.Destination === '/var/lib/postgresql/data'))
      fail('Container PostgreSQL ou volume divergente');
  }
  const io = {compose, verifyAttachments, mode, preserveDatabase,
    waitPg: async () => {for (let n=0;n<60;n++) {
      if (container('postgres').State.Health?.Status === 'healthy') return;
      await sleep(1000);
    } fail('PostgreSQL nao ficou saudavel; ambiente preservado');},
    verifyDatabase: () => {
      const result = compose('exec','-T','postgres','psql','-X','-At','-v','ON_ERROR_STOP=1',
        '-U','provisionador','-d',state.vars.OP_DB_NAME,'-c','SELECT id FROM ops_guard.execution');
      if (result !== state.id) fail('Banco nao pertence a esta apresentacao');
    },
    waitReady: async () => {const ca = transport === 'https' ? fs.readFileSync(path.join(directory,'ca.crt')) : null;
      for (let n=0;n<60;n++) {if (await requestReady(ca)) return; await sleep(1000);}
      fail('API nao ficou disponivel no transporte escolhido');}
  };
  if (action === 'bootstrap') {
    await bootstrapAdmin(state, option === '--primeiro-admin', {...io, save:()=>save(directory,state),
      webRunning:()=>['backend','frontend'].some(service=>{
        const id = compose('ps','-a','-q',service);
        return id && inspect('container',id).State.Running;
      })});
    console.log('Bootstrap concluido; backend normal nao recebe a senha inicial.'); return;
  }
  if (action === 'start' && !argv.includes('--autorizar-migrations')) fail('start exige --autorizar-migrations no banco exclusivo');
  if (action === 'start') {
    if (transport === 'https') {
      command(OPENSSL, certificateArgs(directory).verify);
      command(OPENSSL,['x509','-in',path.join(directory,'server.crt'),'-checkend','86400','-noout']);
    }
    io.switching=state.activeMode!==mode || (state.activeTransport || 'https') !== transport || Boolean(state.pendingMode);
    state.pendingMode=mode;
    state.pendingTransport=transport;
    save(directory,state);
  }
  await lifecycle(action,state,io);
  if(action==='start') {
    verifyAttachments();
    state.activeMode=mode;
    state.activeTransport=transport;
    delete state.pendingMode;
    delete state.pendingTransport;
    save(directory,state);
  }
  console.log(action === 'start' ? 'API disponivel em '+transport+'; login e navegador ainda devem ser conferidos.' : 'Etapa concluida; recursos e segredos preservados.');
}
module.exports = {externalDirectory,preflight,subnetRange,overlaps,owned,composeArgs,lifecycle,bootstrapAdmin,
  PINS,certificateArgs,requireMode,validateTopology,switchMode,validateAttachments,resourcePlan,requireTransport,validateFrontendBinding,verifyHttpReceipt,validateCadastralIdentity,validateFrontendDeploymentIdentity,saveFrontendReceipt};
if (require.main === module) main().catch(error => {
  // Mensagens de bibliotecas podem incluir conteudo privado; somente erros controlados acima sao publicados.
  const controlled = error.stack?.includes('at fail ');
  console.error(controlled ? error.message : 'Etapa interrompida; consultar runbook. Nenhum descarte automatico.');
  process.exitCode = 1;
});
