'use strict';
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');
const {spawnSync} = require('node:child_process');
const https = require('node:https');
const assert = require('node:assert/strict');
const ROOT = path.resolve(__dirname, '..');
const DOCKER = 'C:/Users/julia/AppData/Local/Programs/DockerDesktop/resources/bin/docker.exe';
const OPENSSL = 'C:/Program Files/Git/usr/bin/openssl.exe';
const LABEL = 'gestaoacoes.operacional.run';
const PINS = {
  postgres: 'postgres@sha256:eee6c6e1b0894fa4fe0075eb054af9fd1cf39a35321c2d4110626cfe506411fb',
  frontend: 'nginx@sha256:0e52bd054126cdc040b1276c0af347aeb02a41b9cfe305d6094767ac30e97949',
  backend: 'eclipse-temurin@sha256:c35794d1348777e6bc310b9b7f420098ef8f37e988199549b545fded79a895a2'
};
function preflight(env) {
  if (env.AUTH_HTTPS_REAL !== 'EXECUTAR_094') throw Error('opt-in AUTH_HTTPS_REAL=EXECUTAR_094 obrigatório');
  for (const name of Object.keys(env)) if (/^(OP_|APP_TRUSTED_PROXY_IP|APP_PUBLIC_ORIGIN|SPRING_|AUTH_(JWT|RESET|BOOTSTRAP)|JAVA_TOOL_OPTIONS|JDK_JAVA_OPTIONS|_JAVA_OPTIONS|DOCKER_|COMPOSE_)/i.test(name))
    throw Error('configuração externa capaz de redirecionar runtime; ambiente recusado');
}
function owned(item, id) {
  if ((item.Labels || item.Config?.Labels || {})[LABEL] !== id) throw Error('propriedade exclusiva não comprovada');
}
function command(exe, args, options={}) {
  const result = spawnSync(exe, args, {cwd:ROOT, encoding:'utf8', timeout:240000,
    maxBuffer:64*1024*1024, windowsHide:true, ...options});
  if (result.status !== 0 || result.error) {
    const text=String(result.stderr||'');
    const category=text.includes('schema "public" already exists')?'schema_public_ja_existe':text.includes('permission denied')?'permissao_negada':'erro_ou_timeout';
    throw Error('Comando operacional falhou: '+path.basename(exe)+' '+args[0]+' ['+category+']');
  }
  return result.stdout;
}
function docker(...args) { return command(DOCKER, ['--context','desktop-linux', ...args]); }
function inspect(kind,id) { return JSON.parse(docker(kind,'inspect',id))[0]; }
const pause = ms => new Promise(resolve=>setTimeout(resolve,ms));

async function main() {
  preflight(process.env);
  assert.equal(command(DOCKER,['context','show']).trim(),'desktop-linux');
  assert.equal(docker('context','inspect','desktop-linux','--format','{{.Endpoints.docker.Host}}').trim(),'npipe:////./pipe/dockerDesktopLinuxEngine');
  assert.equal(docker('version','--format','{{.Server.Os}}/{{.Server.Arch}}').trim(),'linux/amd64');
  for (const pin of Object.values(PINS)) assert.equal(inspect('image',pin).Architecture,'amd64');
  const {lookup} = require('node:dns/promises');
  const ips = await lookup('gestaoacoes.localhost',{all:true});
  assert(ips.some(ip=>ip.address==='127.0.0.1'));
  const id=crypto.randomUUID(), project='ga094'+id.replaceAll('-','').slice(0,12);
  const privateDir=fs.mkdtempSync(path.join(os.tmpdir(),'gestaoacoes-https-'));
  assert(!privateDir.toLowerCase().startsWith(ROOT.toLowerCase()));
  const sid=command('whoami.exe',['/user','/fo','csv','/nh']).match(/S-1-5-[\d-]+/)[0];
  command('icacls.exe',[privateDir,'/inheritance:r','/grant:r',`*${sid}:(OI)(CI)F`,'*S-1-5-18:(OI)(CI)F']);
  const evidenceDir=path.join(ROOT,'target/auth-https',id);fs.mkdirSync(evidenceDir,{recursive:true});
  const resources=[], images=[], checks=[];
  const manifest={id, project, privateDir, context:'desktop-linux', pins:PINS, origin:'https://gestaoacoes.localhost:8443',resources,images,checks};
  const save=()=>fs.writeFileSync(path.join(evidenceDir,'manifesto.json'),JSON.stringify(manifest,null,2)+'\n');
  const pass=name=>{checks.push({name,result:'PASS'});save();console.log('PASS '+name);};
  const secret=(name,value)=>fs.writeFileSync(path.join(privateDir,name),value,{mode:0o600});
  const random=()=>crypto.randomBytes(48).toString('base64');
  const adminPassword=random(), userPassword=random();
  for(const [name,value] of Object.entries({'auth.jwt.secret':random(),'auth.reset.secret':random(),'app-password':random(),
    'provision-password':random(),'AUTH_BOOTSTRAP_PASSWORD':adminPassword}))secret(name,value);
  const db='homologacao', app='aplicacao', admin='admin_homologacao';
  const envfile=path.join(privateDir,'runtime.env');
  const vars={OP_PROJECT:project,OP_RUN_ID:id,OP_PRIVATE_DIR:privateDir.replaceAll('\\','/'),OP_DB_NAME:db,OP_DB_APP_USER:app,
    OP_BOOTSTRAP_USER:admin,OP_INGRESS_NETWORK:project+'_ingress',OP_API_NETWORK:project+'_api',
    OP_DATABASE_NETWORK:project+'_database',OP_DATA_VOLUME:project+'_data'};
  const compose=(...args)=>command(DOCKER,['--context','desktop-linux','compose','--env-file',envfile,'-f','compose.operacional.yaml','-p',project,...args]);
  const container = service => {
    const cid=compose('ps','-a','-q',service).trim();assert(cid && !cid.includes('\n'));
    const item=inspect('container',cid);owned(item,id);assert.equal(item.Config.Labels['com.docker.compose.project'],project);
    if(!resources.some(r=>r.kind==='container'&&r.id===cid)){resources.push({kind:'container',id:cid,name:item.Name});save();}
    return cid;
  };
  let pg, backend;
  const sql=(database,text)=>docker('exec','-i',pg,'psql','-X','-q','-t','-A','-v','ON_ERROR_STOP=1','-U','provisionador','-d',database,
    '-c',text).trim();
  try {
    // Certificados locais exclusivos. A CA jamais entra no trust store do Windows.
    command(OPENSSL,['req','-x509','-newkey','rsa:3072','-nodes','-days','2','-subj','/CN=GestaoAcoes Homologacao CA',
      '-keyout',path.join(privateDir,'ca.key'),'-out',path.join(privateDir,'ca.crt')],{env:{...process.env,MSYS_NO_PATHCONV:'1'}});
    command(OPENSSL,['req','-newkey','rsa:3072','-nodes','-subj','/CN=gestaoacoes.localhost',
      '-keyout',path.join(privateDir,'server.key'),'-out',path.join(privateDir,'server.csr')]);
    secret('extensions.cnf','subjectAltName=DNS:gestaoacoes.localhost\nbasicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\n');
    command(OPENSSL,['x509','-req','-days','2','-in',path.join(privateDir,'server.csr'),'-CA',path.join(privateDir,'ca.crt'),
      '-CAkey',path.join(privateDir,'ca.key'),'-CAcreateserial','-extfile',path.join(privateDir,'extensions.cnf'),'-out',path.join(privateDir,'server.crt')]);
    command(OPENSSL,['verify','-CAfile',path.join(privateDir,'ca.crt'),'-verify_hostname','gestaoacoes.localhost',path.join(privateDir,'server.crt')]);
    pass('CA isolada e SAN válido');
    for(const service of ['backend','frontend','postgres']) {
      const dir=path.join(privateDir,'build-'+service);fs.mkdirSync(dir);
      fs.copyFileSync(path.join(ROOT,'ops',service+'.Dockerfile'),path.join(dir,'Dockerfile'));
      if(service==='backend')fs.copyFileSync(path.join(ROOT,'target/gestaoacoes-0.0.1-SNAPSHOT.jar'),path.join(dir,'app.jar'));
      if(service==='frontend') {
        fs.cpSync(path.join(ROOT,'frontend/dist/frontend/browser'),path.join(dir,'browser'),{recursive:true});
        fs.copyFileSync(path.join(ROOT,'ops/nginx.conf'),path.join(dir,'nginx.conf'));
      }
      if(service==='postgres')fs.copyFileSync(path.join(ROOT,'ops/postgres-init.sh'),path.join(dir,'postgres-init.sh'));
      const tag=project+'-'+service+':homologacao';
      docker('build','--platform','linux/amd64','--label',LABEL+'='+id,'--build-arg','RUNTIME_IMAGE='+PINS[service],'-t',tag,dir);
      const image=inspect('image',tag);owned(image,id);images.push({tag,id:image.Id});save();
      vars['OP_'+service.toUpperCase()+'_IMAGE']=tag;
    }
    // Somente redes/volumes novos. Nenhuma seleção por prefixo para exclusão.
    const existing=JSON.parse(docker('network','inspect',...docker('network','ls','-q').trim().split(/\s+/)));
    let octet=180;
    while(existing.some(n=>JSON.stringify(n.IPAM.Config).includes('172.30.'+octet+'.')))octet++;
    assert(octet<250);const subnet='172.30.'+octet+'.0/24';vars.APP_TRUSTED_PROXY_IP='172.30.'+octet+'.10';
    for(const [type,name] of [['ingress',vars.OP_INGRESS_NETWORK],['api',vars.OP_API_NETWORK],['database',vars.OP_DATABASE_NETWORK]]) {
      const args=['network','create','--label',LABEL+'='+id];
      if(type==='api')args.push('--internal','--subnet',subnet);
      if(type==='database')args.push('--internal');
      const rid=docker(...args,name).trim();resources.push({kind:'network',id:rid,name});save();
    }
    const volume=docker('volume','create','--label',LABEL+'='+id,vars.OP_DATA_VOLUME).trim();
    resources.push({kind:'volume',id:volume,name:volume});save();
    fs.writeFileSync(envfile,Object.entries(vars).map(([k,v])=>k+'='+v).join('\n')+'\n');
    const config=JSON.parse(compose('config','--format','json'));
    assert(!config.services.backend.ports && !config.services.postgres.ports);
    assert.equal(config.services.backend.environment.SPRING_PROFILES_ACTIVE,'operacional');
    assert.equal(config.services.backend.environment.OP_DB_NAME,db);
    assert.equal(config.services.backend.environment.OP_DB_APP_USER,app);
    assert.equal(config.services.backend.environment.APP_TRUSTED_PROXY_IP,vars.APP_TRUSTED_PROXY_IP);
    assert(!JSON.stringify(config.services.backend).includes('AUTH_BOOTSTRAP_PASSWORD'));
    compose('create','--no-build','--pull','never','postgres','backend','frontend');
    pg=container('postgres');backend=container('backend');const frontend=container('frontend');
    for(const resource of resources)owned(inspect(resource.kind,resource.id),id);
    const pgInfo=inspect('container',pg);
    assert(!pgInfo.HostConfig.PortBindings || Object.keys(pgInfo.HostConfig.PortBindings).length===0);
    compose('start','postgres');
    for(let n=0;;n++){if(inspect('container',pg).State.Health.Status==='healthy')break;if(n>=60)throw Error('PostgreSQL indisponível');await pause(1000);}
    assert.equal(sql(db,'SELECT id FROM ops_guard.execution'),id);
    assert.equal(sql(db,"SELECT current_database() || '|' || current_user"),db+'|provisionador');
    assert.equal(sql(db,"SELECT rolsuper OR rolcreatedb OR rolcreaterole FROM pg_roles WHERE rolname='aplicacao'"),'f');
    assert.equal(sql(db,'SHOW server_version'),'17.11');
    pass('manifesto, banco novo, roles separadas e portas privadas antes das migrations');
    compose('--profile','bootstrap','create','--no-build','--pull','never','bootstrap');
    const bootstrap=container('bootstrap');
    for(const resource of resources.filter(r=>r.kind==='network')) {
      const network=inspect('network',resource.id);owned(network,id);
      for(const cid of Object.keys(network.Containers||{}))owned(inspect('container',cid),id);
    }
    compose('--profile','bootstrap','start','bootstrap');
    for(let n=0;;n++){const state=inspect('container',bootstrap).State;if(!state.Running){assert.equal(state.ExitCode,0);break;}if(n>=120)throw Error('Bootstrap sem término');await pause(1000);}
    docker('rm',bootstrap);resources.find(r=>r.id===bootstrap).removed=true;save();
    pass('bootstrap one-shot sem credenciais no backend normal');
    compose('start','backend','frontend');
    const ca=fs.readFileSync(path.join(privateDir,'ca.crt'));
    async function http(client,method,url,body,extra={}) {
      const headers={Host:'gestaoacoes.localhost:8443',Origin:'https://gestaoacoes.localhost:8443',...extra};
      if(client) {headers.Cookie=Object.entries(client).map(([k,v])=>k+'='+v).join('; ');if(method!=='GET'&&client['XSRF-TOKEN'])headers['X-XSRF-TOKEN']=decodeURIComponent(client['XSRF-TOKEN']);}
      Object.assign(headers,extra);
      const data=body===undefined?null:JSON.stringify(body);if(data)headers['Content-Type']='application/json';
      return new Promise((resolve,reject)=>{
        const req=https.request({hostname:'gestaoacoes.localhost',port:8443,family:4,servername:'gestaoacoes.localhost',ca,
          method,path:url,headers,timeout:10000},res=>{let text='';res.on('data',b=>text+=b);res.on('end',()=>{
            if(client)for(const cookie of res.headers['set-cookie']||[]){const [name,value]=cookie.split(';')[0].split('=');if(value)client[name]=value;else delete client[name];}
            let json;try{json=JSON.parse(text);}catch{} resolve({status:res.statusCode,headers:res.headers,json,text});
          });});req.on('error',()=>reject(Error('Conexão HTTPS falhou')));req.on('timeout',()=>req.destroy());if(data)req.write(data);req.end();
      });
    }
    for(let n=0;;n++){try{if((await http(null,'GET','/api/auth/csrf')).status===204)break;}catch{}if(n>=120)throw Error('HTTPS/backend indisponível');await pause(1000);}
    const client={};const csrf=async c=>assert.equal((await http(c,'GET','/api/auth/csrf')).status,204);
    const login=async(c,username,senha)=>{await csrf(c);const result=await http(c,'POST','/api/auth/login',{username,senha});assert.equal(result.status,200);await csrf(c);return result;};
    const logged=await login(client,admin,adminPassword);
    const cookie=logged.headers['set-cookie'].find(c=>c.startsWith('GESTAOACOES_AUTH='));
    for(const attr of ['HttpOnly','Secure','SameSite=Lax','Path=/'])assert(cookie.includes(attr));
    const maxAge=Number(cookie.match(/Max-Age=(\d+)/)[1]);assert(maxAge>0 && maxAge<=14400);
    assert(!cookie.includes('Domain='));assert(!logged.text.includes(client.GESTAOACOES_AUTH));
    const before=(await http(client,'GET','/api/auth/me')).json;
    const reload=(await http({...client},'GET','/api/auth/me')).json;
    assert.equal(before.ultimaAtividadeEm,reload.ultimaAtividadeEm);assert.equal(before.expiraEm,reload.expiraEm);
    pass('TLS verificado, cookie protegido, CSRF, login e reload sem renovação');
    assert.equal((await http(null,'GET','/api/carteiras')).status,401);
    assert.equal((await http(client,'POST','/api/carteiras',{nome:'Financeiro sintético'},{'X-XSRF-TOKEN':'invalido'})).status,403);
    assert.equal((await http(client,'POST','/api/auth/logout',{}, {'X-XSRF-TOKEN':''})).status,403);
    const wallet=await http(client,'POST','/api/carteiras',{nome:'Financeiro sintético'});
    assert.equal(wallet.status,201);assert(wallet.headers.location.includes('/api/carteiras/'));
    assert(wallet.headers.location.startsWith('https://gestaoacoes.localhost:8443/'));
    const created=await http(client,'POST','/api/usuarios',{username:'leitor_homologacao',papel:'USER'});
    assert.equal(created.status,201);
    const userLocation=new URL(created.headers.location,'https://gestaoacoes.localhost:8443');
    assert.equal(userLocation.origin,'https://gestaoacoes.localhost:8443');assert(userLocation.pathname.startsWith('/api/usuarios/'));
    const publicClient={};await csrf(publicClient);
    const confirmation={username:'leitor_homologacao',codigo:created.json.autorizacao.codigo,senha:userPassword,confirmacao:userPassword};
    assert.equal((await http(publicClient,'POST','/api/auth/redefinicoes/confirmar',confirmation)).status,204);
    const user={};await login(user,'leitor_homologacao',userPassword);
    assert.equal((await http(user,'GET','/api/carteiras')).status,200);
    assert.equal((await http(user,'POST','/api/carteiras',{nome:'Negada'})).status,403);
    assert.equal((await http(user,'GET','/api/usuarios')).status,403);
    pass('ADMIN escreve; USER consulta; contratos financeiros e Location preservados');
    assert.equal((await http(client,'GET','/api/auth/csrf',undefined,{Host:'invasor.localhost:8443'})).status,421);
    assert.equal((await http(client,'GET','/api/auth/csrf',undefined,{Host:'gestaoacoes.localhost'})).status,421);
    assert.equal((await http(client,'POST','/api/auth/atividade',{}, {Origin:'https://gestaoacoes.localhost'})).status,403);
    assert.equal((await http(client,'POST','/api/auth/atividade',{}, {Origin:'https://evil.example'})).status,403);
    assert.equal((await http(client,'GET','/api/auth/me',undefined,{'X-Forwarded-Proto':'http','X-Forwarded-Host':'evil.example','X-Forwarded-Port':'80','X-Forwarded-For':'198.51.100.1',Forwarded:'for=198.51.100.1;proto=http'})).status,200);
    const openapi=await http(null,'GET','/api/v3/api-docs');assert.equal(openapi.status,200);assert.equal(openapi.json.servers[0].url,'/api');
    assert.equal((await http(null,'GET','/api/swagger-ui/index.html')).status,200);
    pass('Host/Origin/porta rejeitados; headers do cliente sobrescritos; OpenAPI mesma origem');
    // Permissionamento efetivo de arquivos no container, não somente bits do bind Windows.
    docker('exec',backend,'sh','-c','test "$(id -u)" = 10001 && test -r /run/secrets/auth.jwt.secret && test ! -w /run/secrets/auth.jwt.secret');
    const unrelated=spawnSync(DOCKER,['--context','desktop-linux','exec','--user','10002:10002',backend,'sh','-c','test -r /run/secrets/auth.jwt.secret'],{windowsHide:true});assert.notEqual(unrelated.status,0);
    pass('segredos somente leitura e inacessíveis a UID alheio');
    // Bloco de segurança/reinício/recuperação é executado abaixo.
    await acceptance({client,user,created,http,csrf,login,sql,db,app,pg,backend,frontend,vars,compose,container,
      privateDir,resources,id,save,pass,admin,adminPassword,userPassword,evidenceDir});
    manifest.result='PASS';save();
  } catch(error) {
    manifest.result='FAIL';manifest.failure=error instanceof assert.AssertionError?'asserção de aceite não atendida':error.message;
    manifest.location=String(error.stack).split('\n').filter(line=>line.trim().startsWith('at ')).slice(0,4);save();
    console.log('FALHA '+manifest.failure+' '+JSON.stringify(manifest.location));
    throw Error('Homologação não concluída; consultar manifesto sanitizado '+id);
  } finally {
    for(const resource of [...resources].reverse()) {
      if(resource.removed)continue;
      try {
        const item=inspect(resource.kind,resource.id);owned(item,id);
        if(resource.kind==='container')docker('rm','-f',resource.id);
        else docker(resource.kind,'rm',resource.id);
        resource.removed=true;
      }catch {resource.cleanupPending=true;}
      save();
    }
    for(const image of [...images].reverse())try{owned(inspect('image',image.tag),id);docker('image','rm',image.tag);image.removed=true;save();}catch{image.cleanupPending=true;save();}
    manifest.cleanupComplete=resources.every(r=>r.removed)&&images.every(i=>i.removed);save();
    if(!manifest.cleanupComplete)process.exitCode=1;
    console.log('EVIDENCIA '+path.relative(ROOT,evidenceDir)+' descarte='+manifest.cleanupComplete);
    // Materiais privados externos preservados para auditoria/recuperação; não copiar para target.
  }
}
async function acceptance(c) {
  const {client,user,created,http,csrf,login,sql,db,app,pg,backend,frontend,vars,compose,container,
    privateDir,resources,id,save,pass,admin,adminPassword}=c;
  // Rejeição TLS real, sem rejectUnauthorized=false nem importação global de CA.
  for(const options of [{}, {ca:fs.readFileSync(path.join(privateDir,'ca.crt')),servername:'nome-incorreto.localhost'}]) {
    await new Promise((resolve,reject)=>{
      const req=https.get({hostname:'gestaoacoes.localhost',family:4,port:8443,path:'/',...options},res=>{res.resume();reject(Error('TLS aceitou confiança ou SAN indevido'));});
      req.on('error',resolve);
    });
  }
  pass('TLS recusa CA não confiável e SAN incompatível');
  const peer=(name,network)=>{
    const cid=docker('create','--name',vars.OP_PROJECT+'-'+name,'--label',LABEL+'='+id,
      '--network',network,'--mount','type=bind,source='+path.join(privateDir,'ca.crt')+',target=/ca.crt,readonly',
      '--entrypoint','sh',vars.OP_FRONTEND_IMAGE,'-c','sleep 1800').trim();
    owned(inspect('container',cid),id);resources.push({kind:'container',id:cid,name});save();docker('start',cid);return cid;
  };
  const ingressIp=inspect('container',frontend).NetworkSettings.Networks[vars.OP_INGRESS_NETWORK].IPAddress;
  const attack=peer('direto',vars.OP_API_NETWORK);
  const denied=docker('exec',attack,'curl','--silent','--show-error','--max-time','10','-o','/tmp/body','-w','%{http_code}',
    '-H','Host: gestaoacoes.localhost:8443','-H','X-Forwarded-Proto: https','-H','X-Forwarded-For: 127.0.0.1',
    '-H','X-Forwarded-Host: gestaoacoes.localhost','-H','X-Forwarded-Port: 8443','http://backend:8080/auth/csrf').trim();
  assert.equal(denied,'400');
  assert(docker('exec',attack,'cat','/tmp/body').includes('HTTPS_OBRIGATORIO'));
  assert.equal(Object.keys(inspect('container',backend).HostConfig.PortBindings||{}).length,0);
  pass('acesso direto sem porta publicada e falso proxy interno negado por Tomcat');
  const a=peer('cliente-a',vars.OP_INGRESS_NETWORK), b=peer('cliente-b',vars.OP_INGRESS_NETWORK);
  const curl=(cid,script)=>docker('exec',cid,'sh','-eu','-c',script).trim();
  const common=`curl --silent --show-error --max-time 10 --cacert /ca.crt --resolve gestaoacoes.localhost:8443:${ingressIp}`;
  const origin='https://gestaoacoes.localhost:8443';
  for(const cid of [a,b])curl(cid,`${common} -c /tmp/jar ${origin}/api/auth/csrf -o /dev/null`);
  const invalid=cid=>curl(cid,`token=$(awk '$6 == "XSRF-TOKEN" {print $7}' /tmp/jar); ${common} -b /tmp/jar -D /tmp/headers -o /dev/null -w '%{http_code}' -H "X-XSRF-TOKEN: $token" -H 'Origin: ${origin}' -H 'X-Forwarded-For: 198.51.100.'$$ -H 'Content-Type: application/json' --data '{"username":"inexistente_rate","senha":"senha-invalida"}' ${origin}/api/auth/login`);
  for(let n=0;n<5;n++)assert.equal(invalid(a),'401');
  assert.equal(invalid(a),'429');assert(/retry-after:/i.test(docker('exec',a,'cat','/tmp/headers')));
  assert.equal(invalid(b),'401');
  const aIp=inspect('container',a).NetworkSettings.Networks[vars.OP_INGRESS_NETWORK].IPAddress;
  const bIp=inspect('container',b).NetworkSettings.Networks[vars.OP_INGRESS_NETWORK].IPAddress;
  assert.notEqual(aIp,bIp);
  // O limiter registra SHA-256 do IP canônico; comparar com IP real observado no Docker.
  for(const ip of [aIp,bIp])assert.equal(sql(db,"SELECT count(*) FROM contador_abuso WHERE tipo='LOGIN_IP' AND chave_hash='"+crypto.createHash('sha256').update(ip).digest('hex')+"'"),'1');
  pass('limitador identifica dois IPs reais distintos e ignora XFF forjado; 429/Retry-After');
  const uid=created.json.usuario.id;
  let current=(await http(client,'GET','/api/usuarios/'+uid)).json;
  const blocked=await http(client,'PATCH','/api/usuarios/'+uid+'/situacao',{ativo:false,versaoEsperada:current.versao});
  assert.equal(blocked.status,200);assert.equal((await http(user,'GET','/api/auth/me')).status,401);
  const enabled=await http(client,'PATCH','/api/usuarios/'+uid+'/situacao',{ativo:true,versaoEsperada:blocked.json.versao});
  assert.equal(enabled.status,200);assert.equal((await http(user,'GET','/api/auth/me')).status,401);
  await login(user,'leitor_homologacao',c.userPassword);
  const oldUser={...user};assert.equal((await http(user,'POST','/api/auth/logout',{})).status,204);
  assert.equal((await http(oldUser,'GET','/api/auth/me')).status,401);
  pass('bloqueio revoga; habilitação não restaura; logout revoga cookie antigo');
  const identity=(await http(client,'GET','/api/auth/me')).json;
  const pending=await http(client,'POST','/api/usuarios',{username:'pendente_homologacao',papel:'USER'});
  assert.equal(pending.status,201);
  const keyHashes=['auth.jwt.secret','auth.reset.secret'].map(f=>crypto.createHash('sha256').update(fs.readFileSync(path.join(privateDir,f))).digest('hex'));
  compose('stop','backend');docker('restart',pg);compose('start','backend');
  for(let n=0;;n++){try{if((await http(client,'GET','/api/auth/me')).status===200)break;}catch{}if(n>=100)throw Error('Reinício indisponível');await pause(1000);}
  const restarted=(await http(client,'GET','/api/auth/me')).json;
  assert.equal(restarted.id,identity.id);assert.equal(restarted.ultimaAtividadeEm,identity.ultimaAtividadeEm);assert.equal(restarted.expiraEm,identity.expiraEm);
  for(const [i,f] of ['auth.jwt.secret','auth.reset.secret'].entries())assert.equal(crypto.createHash('sha256').update(fs.readFileSync(path.join(privateDir,f))).digest('hex'),keyHashes[i]);
  assert.equal(sql(db,"SELECT count(*) FROM autorizacao_redefinicao_senha WHERE consumida_em IS NULL AND invalidada_em IS NULL"),'1');
  pass('reinício PostgreSQL e aplicação preserva sessão, prazos, códigos e chaves');
  // Banco parado para escritas durante o backup sintético. Nunca há dump aberto em disco.
  compose('stop','backend');
  const tables=sql(db,"SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename").split('\n');
  const snapshots=tables.map(t=>sql(db,`SELECT md5(COALESCE(jsonb_agg(to_jsonb(t) ORDER BY to_jsonb(t)::text)::text,'[]')) FROM public.${t} t`));
  const dump=command(DOCKER,['--context','desktop-linux','exec',pg,'pg_dump','-U','provisionador','-d',db,'-Fc','--schema=public','--no-owner','--no-privileges'],{encoding:null});
  const backupKey=crypto.randomBytes(32),nonce=crypto.randomBytes(12),cipher=crypto.createCipheriv('aes-256-gcm',backupKey,nonce);
  const encrypted=Buffer.concat([cipher.update(dump),cipher.final()]);
  fs.writeFileSync(path.join(privateDir,'backup.key'),backupKey,{mode:0o600});
  fs.writeFileSync(path.join(privateDir,'backup.enc'),Buffer.concat([nonce,cipher.getAuthTag(),encrypted]),{mode:0o600});
  const restored='restauracao';sql('postgres',`CREATE DATABASE ${restored} OWNER ${app}`);
  assert.equal(sql(restored,"SELECT count(*) FROM pg_tables WHERE schemaname='public'"),'0');
  sql(restored,`CREATE SCHEMA ops_guard; CREATE TABLE ops_guard.execution(id uuid PRIMARY KEY); INSERT INTO ops_guard.execution VALUES('${id}')`);
  assert.equal(sql(restored,'SELECT id FROM ops_guard.execution'),id);
  // Apenas schema vazio do banco recém-criado, sem CASCADE e antes do restore.
  // O dump contém CREATE SCHEMA public; nunca se aplica a banco existente.
  sql(restored,'DROP SCHEMA public');
  const file=fs.readFileSync(path.join(privateDir,'backup.enc')),decipher=crypto.createDecipheriv('aes-256-gcm',backupKey,file.subarray(0,12));
  decipher.setAuthTag(file.subarray(12,28));const plain=Buffer.concat([decipher.update(file.subarray(28)),decipher.final()]);
  command(DOCKER,['--context','desktop-linux','exec','-i',pg,'pg_restore','-U','provisionador','-d',restored,'--role',app,
    '--single-transaction','--no-owner','--no-privileges','--exit-on-error'],{input:plain,encoding:null});
  for(const [i,t] of tables.entries())assert.equal(sql(restored,`SELECT md5(COALESCE(jsonb_agg(to_jsonb(t) ORDER BY to_jsonb(t)::text)::text,'[]')) FROM public.${t} t`),snapshots[i]);
  assert.equal(sql(restored,'SELECT count(*) FROM databasechangelog'),sql(db,'SELECT count(*) FROM databasechangelog'));
  pass('backup AES-256-GCM externo restaurado em outro banco: financeiro, auditoria e Liquibase idênticos');
  sql(restored,`BEGIN; SELECT id FROM controle_seguranca FOR UPDATE; SELECT id FROM usuario ORDER BY id FOR UPDATE;
    SELECT id FROM autorizacao_redefinicao_senha ORDER BY id FOR UPDATE; SELECT sid FROM sessao_autenticacao ORDER BY sid FOR UPDATE;
    UPDATE usuario SET versao_seguranca=versao_seguranca+1,versao=versao+1,atualizada_em=CURRENT_TIMESTAMP;
    UPDATE autorizacao_redefinicao_senha SET invalidada_em=CURRENT_TIMESTAMP,versao=versao+1 WHERE consumida_em IS NULL AND invalidada_em IS NULL;
    UPDATE sessao_autenticacao SET revogada_em=CURRENT_TIMESTAMP,motivo_revogacao='RECUPERACAO',versao=versao+1 WHERE revogada_em IS NULL; COMMIT;`);
  // Chaves novas somente na recuperação, nunca no reinício normal.
  fs.writeFileSync(path.join(privateDir,'auth.jwt.secret'),crypto.randomBytes(48).toString('base64'));
  fs.writeFileSync(path.join(privateDir,'auth.reset.secret'),crypto.randomBytes(48).toString('base64'));
  vars.OP_DB_NAME=restored;
  fs.writeFileSync(path.join(privateDir,'runtime.env'),Object.entries(vars).map(([k,v])=>k+'='+v).join('\n')+'\n');
  // Remover só o container parado confirmado; o volume original permanece até o descarte final.
  owned(inspect('container',backend),id);docker('rm',backend);resources.find(r=>r.id===backend).removed=true;save();
  compose('create','--no-build','--pull','never','backend');const recovered=container('backend');compose('start','backend');
  // Nginx resolve o upstream no start; recriação de container exige nova resolução.
  compose('restart','frontend');
  for(let n=0;;n++){try{if((await http(null,'GET','/api/auth/csrf')).status===204)break;}catch{}if(n>=100)throw Error('Recuperação indisponível');await pause(1000);}
  assert.equal((await http(client,'GET','/api/auth/me')).status,401);
  const resetClient={};await csrf(resetClient);
  assert.equal((await http(resetClient,'POST','/api/auth/redefinicoes/confirmar',{username:'pendente_homologacao',codigo:pending.json.autorizacao.codigo,senha:c.userPassword,confirmacao:c.userPassword})).status,400);
  const restoredAdmin={};await login(restoredAdmin,admin,adminPassword);
  assert.equal((await http(restoredAdmin,'GET','/api/carteiras')).status,200);
  assert.equal(sql(restored,"SELECT count(*) FROM sessao_autenticacao WHERE revogada_em IS NULL AND usuario_id != "+identity.id),'0');
  pass('recuperação nega sessões e códigos restaurados; schema validado; novo login funcional');
  assert.equal(inspect('container',recovered).State.Running,true);
}
module.exports = {preflight, owned};
if(require.main===module)main().catch(error=>{console.error(error instanceof assert.AssertionError?'Preflight: asserção de isolamento não atendida':error.message);process.exitCode=1;});
