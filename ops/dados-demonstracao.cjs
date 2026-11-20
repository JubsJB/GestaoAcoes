'use strict';
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawnSync} = require('node:child_process');
const {isDeepStrictEqual} = require('node:util');
const {externalDirectory, preflight, validateFrontendBinding} = require('./apresentacao.cjs');

const origem = 'http://localhost:8081';
const perfil = 'dados-demonstracao';
const carteiras = {crescimento:'Carteira de Crescimento', equilibrio:'Carteira Conservadora'};
const nomesAnteriores = ['demo — crescimento','demo — equilíbrio'];
const acoes = [
  {ticker:'DEMA3', nome:'Demo Aurora', mercado:'BRASIL', moeda:'BRL', preco:24},
  {ticker:'DEMB4', nome:'Demo Horizonte', mercado:'BRASIL', moeda:'BRL', preco:27},
  {ticker:'ZDMO', nome:'Demo Orbit', mercado:'EUA', moeda:'USD', preco:120}
];
const corretoras = [
  {cnpj:'99999999000191', nome:'Corretora Demo Norte', cep:'99999001', logradouro:'Rua Fictícia Norte'},
  {cnpj:'99999998000147', nome:'Corretora Demo Sul', cep:'99999002', logradouro:'Rua Fictícia Sul'}
];
const exigir = (condicao, mensagem) => {if (!condicao) throw Error(mensagem);};
const mesmo = (a,b,mensagem) => exigir(isDeepStrictEqual(a,b), mensagem);
const digitos = valor => String(valor).replace(/\D/g,'');

function operacoes() {
  return [
    ['crescimento','DEMA3',0,'COMPRA',100,20,'2026-11-10'],
    ['crescimento','DEMB4',1,'COMPRA',10,30,'2026-11-10'],
    ['crescimento','ZDMO',0,'COMPRA',2.5,100,'2026-11-10'],
    ['equilibrio','DEMB4',1,'COMPRA',20,30,'2026-11-10'],
    ['equilibrio','ZDMO',1,'COMPRA',1.5,110,'2026-11-10'],
    ['crescimento','DEMA3',0,'COMPRA',50,26,'2026-11-12'],
    ['crescimento','DEMA3',0,'VENDA',40,28,'2026-11-17'],
    ['equilibrio','DEMB4',1,'VENDA',5,28,'2026-11-17']
  ].map(([carteira,ticker,corretora,tipo,quantidade,precoUnitario,dataOperacao])=>({
    carteira,ticker,corretora,tipo,quantidade,precoUnitario,dataOperacao
  }));
}

async function etapa(state, chave, salvar, enviar) {
  exigir(!state.pendente, 'Há uma escrita pendente. Concilie antes de retomar.');
  state.pendente = chave;
  salvar();
  const resposta = await enviar();
  exigir(Number.isSafeInteger(resposta.id) && resposta.id > 0, 'Resposta sem ID verificável.');
  state.ids ||= {};
  state.ids[chave] = resposta.id;
  delete state.pendente;
  salvar();
}

function conferirPrefixo(atuais, plano, ids) {
  exigir(atuais.length === ids.length && new Set(ids).size === ids.length, 'Histórico diverge do diário.');
  for (const [index,id] of ids.entries()) {
    const atual = atuais.find(item => item.id === id);
    exigir(atual && Object.entries(plano[index]).every(([campo,valor])=>atual[campo] === valor),
      'Registro diverge do plano.');
  }
}

function identidade(dir) {
  preflight(process.env);
  const m = JSON.parse(fs.readFileSync(path.join(dir,'manifesto.json'),'utf8'));
  exigir(m.version === 2 && /^gademo[a-f0-9]{12}$/.test(m.project)
    && /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(m.id)
    && m.project === 'gademo'+m.id.replaceAll('-','').slice(0,12)
    && m.vars.OP_DB_NAME === 'demonstracao' && m.vars.OP_PROJECT === m.project
    && m.vars.OP_PRIVATE_DIR.toLowerCase() === dir.replaceAll('\\','/').toLowerCase()
    && m.bootstrapCompleted && m.activeMode === 'sem-integracoes' && m.activeTransport === 'http-local'
    && !m.pendingMode && !m.pendingTransport, 'O manifesto não identifica uma apresentação HTTP offline confirmada.');
  const docker = (...args) => {
    const r = spawnSync('docker',['--context','desktop-linux',...args],{encoding:'utf8',timeout:10000});
    exigir(r.status === 0, 'Inspeção local do Docker falhou.');
    return r.stdout.trim();
  };
  const context = docker('context','inspect','desktop-linux','--format','{{.Endpoints.docker.Host}}');
  mesmo(context,'npipe:////./pipe/dockerDesktopLinuxEngine','Docker precisa ser local.');
  const networks = {
    postgres:[m.vars.OP_DATABASE_NETWORK],
    backend:[m.vars.OP_DATABASE_NETWORK,m.vars.OP_API_NETWORK],
    frontend:[m.vars.OP_INGRESS_NETWORK,m.vars.OP_API_NETWORK]
  };
  for (const [servico, redes] of Object.entries(networks)) {
    const nome = m.project+'-'+servico+'-1';
    const formato = '{{.State.Status}}|{{json .State.Health}}|{{json .Config.Labels}}|{{json .NetworkSettings.Networks}}|{{json .HostConfig.PortBindings}}|{{json .Mounts}}|{{.Image}}';
    const [state, health, labels, links, ports, mounts, image] = docker('inspect','--format',formato,nome).split('|');
    exigir(state === 'running' && JSON.parse(labels)['gestaoacoes.operacional.run'] === m.id
      && JSON.parse(labels)['com.docker.compose.project'] === m.project, 'Container não corresponde ao manifesto.');
    mesmo(Object.keys(JSON.parse(links)).sort(),redes.sort(),'Redes inesperadas na apresentação.');
    if(servico === 'frontend') validateFrontendBinding(JSON.parse(ports),'http-local');
    else exigir(Object.values(JSON.parse(ports) || {}).every(v => !v?.length),'Publicação indevida.');
    if(servico === 'backend') mesmo(image,docker('image','inspect','--format','{{.Id}}',m.project+'-backend:dados-demonstracao'),'Imagem demonstrativa divergente.');
    if(servico === 'postgres') exigir(JSON.parse(health)?.Status === 'healthy' && image === m.imageIds.postgres
      && JSON.parse(mounts).some(v=>v.Type === 'volume' && v.Name === m.vars.OP_DATA_VOLUME
        && v.Destination === '/var/lib/postgresql/data')
      && m.resources[m.vars.OP_DATA_VOLUME] === m.vars.OP_DATA_VOLUME,'Banco ou volume divergente.');
  }
  for(const rede of [m.vars.OP_DATABASE_NETWORK,m.vars.OP_API_NETWORK]){
    const intern = docker('network','inspect','--format','{{.Id}}|{{.Internal}}',rede);
    mesmo(intern,m.resources[rede]+'|true','Redes internas divergentes do manifesto.');
  }
  const volume = docker('volume','inspect','--format','{{index .Labels "gestaoacoes.operacional.run"}}',m.vars.OP_DATA_VOLUME);
  mesmo(volume,m.id,'Volume não pertence à apresentação.');
  const marker = docker('exec','-e','PGOPTIONS=-c default_transaction_read_only=on',m.project+'-postgres-1',
    'psql','-X','-At','-v','ON_ERROR_STOP=1','-U','provisionador','-d','demonstracao',
    '-c','SELECT id FROM ops_guard.execution');
  mesmo(marker,m.id,'O banco não corresponde à apresentação.');
  return m.id;
}

function clienteHttp(port = 8081) {
  const cookies = new Map();
  return async (method, route, body) => {
    const headers = {Host:'localhost:8081',Origin:origem};
    if(cookies.size) headers.Cookie = [...cookies].map(([key,value])=>key+'='+value).join('; ');
    if(method === 'POST') {
      headers['X-XSRF-TOKEN'] = cookies.get('XSRF-TOKEN') || '';
      headers['Content-Type'] = 'application/json';
    }
    const payload = body === undefined ? undefined : JSON.stringify(body);
    if(payload !== undefined) headers['Content-Length'] = Buffer.byteLength(payload);
    const response = await new Promise((resolve,reject)=>{
      const request = http.request({hostname:'127.0.0.1',port,path:'/api'+route,method,headers,
        signal:AbortSignal.timeout(10000)}, res=>{
        const chunks = [];
        let bytes = 0;
        res.on('data',chunk=>{
          bytes += chunk.length;
          if(bytes > 4*1024*1024) res.destroy(Error('Resposta excede o limite local.'));
          else chunks.push(chunk);
        });
        res.on('error',reject);
        res.on('end',()=>resolve({status:res.statusCode, cookies:res.headers['set-cookie'] || [],
          body:Buffer.concat(chunks).toString('utf8')}));
      });
      request.on('error',reject);
      request.end(payload);
    });
    for (const cookie of response.cookies) {
      const [pair,...attrs] = cookie.split(';');
      const index = pair.indexOf('=');
      const key = pair.slice(0,index);
      if(!['XSRF-TOKEN','GESTAOACOES_AUTH'].includes(key)) continue;
      if(attrs.some(a=>a.trim().toLowerCase()==='max-age=0')) cookies.delete(key);
      else cookies.set(key,pair.slice(index+1));
    }
    exigir(response.status >= 200 && response.status < 300,'API recusou uma etapa (HTTP '+response.status+').');
    return response.status === 204 ? null : JSON.parse(response.body);
  };
}

async function preparar(api, execucao, state, salvar, escrever) {
  const remoto = await api('GET','/apresentacao/catalogo');
  mesmo(remoto,{perfil,execucao,acoes,corretoras},'Identidade, perfil ou catálogo divergente.');
  exigir(!state.pendente,'Há uma escrita pendente. Consulte o estado antes de qualquer nova carga.');
  const ids = state.ids ||= {};
  const users = (await api('GET','/usuarios')).filter(item=>item.username==='julia');
  exigir(users.length <= 1 && (!users.length || users[0].papel==='USER' && users[0].ativo),
    'Conta julia inesperada.');
  if(escrever) exigir(users[0]?.credencialDefinida,'Finalize a senha de julia pelo fluxo normal primeiro.');
  if(state.usuario !== undefined) mesmo(users[0]?.id,state.usuario,'Identidade de julia mudou.');
  const recursos = [
    ...corretoras.map((item,i)=>({
      key:'corretora'+i,rota:'/corretoras',body:{cnpj:item.cnpj},
      seleciona:r=>digitos(r.cnpj)===item.cnpj,
      valida:r=>r.nomeFantasia===item.nome && r.razaoSocial===item.nome+' (fictícia)' &&
        digitos(r.cep)===item.cep && r.logradouro===item.logradouro && r.numero==='1' &&
        r.bairro==='Bairro Fictício' && r.cidade==='São Paulo' && r.uf==='SP' &&
        r.situacaoCadastral==='ATIVA' && r.validadaMercadoFinanceiro===false
    })),
    ...acoes.map(item=>({
      key:item.ticker,rota:'/acoes',body:{ticker:item.ticker,mercado:item.mercado},
      seleciona:r=>r.ticker===item.ticker && r.mercado===item.mercado,
      valida:r=>r.nomeEmpresa===item.nome && r.moeda===item.moeda && r.cotacaoAtual===item.preco
    })),
    ...Object.entries(carteiras).map(([key,nome])=>({
      key,rota:'/carteiras',body:{nome},seleciona:r=>r.nome.trim().toLowerCase()===nome.toLowerCase(),valida:r=>r.nome===nome
    }))
  ];
  const listas = {};
  for(const recurso of recursos){
    listas[recurso.rota] ||= await api('GET',recurso.rota);
    if(recurso.rota === '/carteiras')
      exigir(!listas[recurso.rota].some(c=>nomesAnteriores.includes(c.nome.trim().toLowerCase())),
        'Carteira com nome anterior do cenário requer conciliação.');
    const achados = listas[recurso.rota].filter(recurso.seleciona);
    exigir(achados.length === (ids[recurso.key] === undefined ? 0 : 1) &&
      achados.every(item=>item.id===ids[recurso.key] && recurso.valida(item)), 'Cadastro colide ou diverge do diário.');
  }
  const plano = operacoes();
  const corpo = item => {
    const {carteira,corretora,...resto} = item;
    return {...resto,carteiraId:ids[carteira],corretoraId:ids['corretora'+corretora],
      mercado:acoes.find(a=>a.ticker===item.ticker).mercado};
  };
  const valores = {crescimento:[[3870,300],[2910,300]], equilibrio:[[540,180],[405,180]]};
  const confereSnapshot = (snapshot, chave, fase) => {
    mesmo(Object.fromEntries(snapshot.patrimonios.map(item=>[item.moeda,item.patrimonioAtual])),
      {BRL:valores[chave][fase][0],USD:valores[chave][fase][1]},'Patrimônio do snapshot diverge do plano.');
  };
  for (const chave of Object.keys(carteiras)) {
    if(!ids[chave]) continue;
    const raiz = '/carteiras/'+ids[chave];
    const itens = plano.map((item,index)=>({item,index})).filter(({item})=>item.carteira===chave);
    const gravados = itens.filter(({index})=>ids['operacao'+index] !== undefined);
    conferirPrefixo(await api('GET',raiz+'/operacoes'),itens.map(({item})=>corpo(item)),
      gravados.map(({index})=>ids['operacao'+index]));
    const {pontos} = await api('GET',raiz+'/evolucao-patrimonial');
    conferirPrefixo(pontos.map(p=>({...p,id:p.snapshotId})),[{},{}],
      [ids[chave+'snapshot0'],ids[chave+'snapshot1']].filter(id=>id!==undefined));
    pontos.forEach((p,i)=>confereSnapshot(p,chave,i));
    exigir((await api('GET',raiz+'/movimentacoes-custodia')).length === 0,'Custódia inesperada no cenário.');
  }
  const ordem = [...recursos.map(r=>r.key),...plano.slice(0,6).map((_,i)=>'operacao'+i),
    'crescimentosnapshot0','equilibriosnapshot0','operacao6','operacao7','crescimentosnapshot1','equilibriosnapshot1'];
  mesmo(Object.keys(ids),ordem.slice(0,Object.keys(ids).length),'Diário fora da sequência aprovada.');
  if(!escrever) return Boolean(users[0]?.credencialDefinida);
  state.usuario = users[0].id;
  const criar = (key,route,body,validar) => ids[key] ? undefined : etapa(state,key,salvar,async()=>{
    const item = await api('POST',route,body);
    exigir(validar(item),'A resposta não corresponde ao cenário.');
    return item;
  });
  for(const r of recursos) await criar(r.key,r.rota,r.body,item=>r.seleciona(item) && r.valida(item));
  for(const [fase,tipo] of ['COMPRA','VENDA'].entries()){
    for(const [index,item] of plano.entries()) if(item.tipo === tipo){
      const payload = corpo(item);
      await criar('operacao'+index,'/operacoes',payload,r=>Object.entries(payload).every(([k,v])=>r[k]===v));
    }
    for(const chave of Object.keys(carteiras))
      await criar(chave+'snapshot'+fase,'/carteiras/'+ids[chave]+'/snapshots',undefined,r=>{
        confereSnapshot(r,chave,fase);
        return r.carteiraId===ids[chave];
      });
  }
  await preparar(api,execucao,state,salvar,false);
  return true;
}

async function main() {
  const [acao, rawDir, ...extras] = process.argv.slice(2);
  exigir(extras.length===0 && ['--verificar','--aplicar'].includes(acao) && rawDir,
    'Uso: node ops/dados-demonstracao.cjs --verificar|--aplicar DIRETORIO');
  const dir = externalDirectory(fs.realpathSync(externalDirectory(rawDir)));
  const lock = path.join(dir,'execucao.lock');
  const fd = fs.openSync(lock,'wx');
  try {
    const execucao = identidade(dir);
    const diario = path.join(dir,'dados-demonstracao-v1.json');
    const state = fs.existsSync(diario) ? JSON.parse(fs.readFileSync(diario,'utf8')) : {execucao,ids:{}};
    mesmo(state.execucao,execucao,'Diário pertence a outra execução.');
    exigir(!state.pendente,'Escrita anterior pendente de conciliação.');
    function salvar(){
      const tmp = diario+'.tmp';
      fs.writeFileSync(tmp,JSON.stringify(state),{flag:'wx',flush:true});
      fs.renameSync(tmp,diario);
    }
    const senha = fs.readFileSync(0,'utf8').replace(/\r?\n$/,'');
    exigir(senha.length>0,'Forneça a senha do ADMIN por stdin privado.');
    const api = clienteHttp();
    await api('GET','/auth/csrf');
    const sessao = await api('POST','/auth/login',{username:'admin_demo',senha});
    exigir(sessao?.papel==='ADMIN','Sessão administrativa esperada.');
    await api('GET','/auth/csrf');
    try {
      const conta = await preparar(api,execucao,state,salvar,acao==='--aplicar');
      console.log(conta ? 'Cenário coerente com o diário.' : 'Pré-voo válido. Prepare julia pela interface.');
    } finally {
      await api('POST','/auth/logout');
    }
  } finally {
    fs.closeSync(fd);
    fs.unlinkSync(lock);
  }
}

module.exports = {etapa, conferirPrefixo, operacoes, preparar, acoes, corretoras, clienteHttp};
if(require.main===module) main().catch(error=>{
  const controlado = /\bat (exigir|mesmo)\b/.test(error.stack);
  console.error(controlado ? error.message : 'Preparação interrompida. Confira rede local, lock e diário privado.');
  process.exitCode=1;
});
