const assert = require('node:assert/strict');
const {test} = require('node:test');
const {etapa, conferirPrefixo, operacoes, preparar, acoes, corretoras, clienteHttp} = require('./dados-demonstracao.cjs');

test('falha ambígua deixa etapa pendente e impede reenvio', async () => {
  const state = {};
  let calls = 0;
  const send = async () => {calls++; throw Error('interrompido');};
  await assert.rejects(etapa(state, 'compra', () => {}, send));
  assert.equal(state.pendente, 'compra');
  await assert.rejects(etapa(state, 'compra', () => {}, send));
  assert.equal(calls, 1);
});

test('histórico deve corresponder exatamente ao prefixo registrado', () => {
  const plano = [{id:12, quantidade:10}, {id:13, quantidade:2.5}];
  conferirPrefixo([plano[0]], plano, [12]);
  assert.throws(() => conferirPrefixo(plano, plano, [12]));
  assert.throws(() => conferirPrefixo([{id:12, quantidade:11}], plano, [12]));
});

test('cenário tem seis compras e duas vendas, inclusive a compra adicional', () => {
  const itens = operacoes();
  assert.equal(itens.filter(item => item.tipo === 'COMPRA').length, 6);
  assert.equal(itens.filter(item => item.tipo === 'VENDA').length, 2);
  assert.ok(itens.some(item => item.carteira === 'crescimento' && item.ticker === 'DEMB4' && item.quantidade === 10));
});

test('carga completa valida e não repete escritas, mesmo após retomada de prefixo confirmado', async () => {
  const state = {ids:{}};
  const rows = {'/usuarios':[{id:2,username:'julia',papel:'USER',ativo:true,credencialDefinida:true}],
    '/acoes':[], '/corretoras':[], '/carteiras':[]};
  let next = 100;
  let writes = 0;
  const snapshot = (carteiraId, fase) => {
    const nome = rows['/carteiras'].find(c=>c.id===carteiraId).nome;
    const a = nome.endsWith('Crescimento');
    return {carteiraId,patrimonios:[{moeda:'BRL',patrimonioAtual:a?[3870,2910][fase]:[540,405][fase]},
      {moeda:'USD',patrimonioAtual:a?300:180}]};
  };
  const getRows = key => rows[key] ||= [];
  async function api(method, route, body){
    if(route==='/apresentacao/catalogo') return {perfil:'dados-demonstracao',execucao:'fixture',acoes,corretoras};
    if(method==='GET'){
      if(route.endsWith('/evolucao-patrimonial')) return {pontos:getRows(route.replace('/evolucao-patrimonial','/snapshots'))};
      return getRows(route);
    }
    writes++;
    let result = {...body};
    if(route==='/corretoras'){
      const c = corretoras.find(c=>c.cnpj===body.cnpj);
      Object.assign(result,c,{nomeFantasia:c.nome,razaoSocial:c.nome+' (fictícia)',numero:'1',
        bairro:'Bairro Fictício',cidade:'São Paulo',uf:'SP',situacaoCadastral:'ATIVA',validadaMercadoFinanceiro:false});
    }
    if(route==='/acoes'){
      const a=acoes.find(a=>a.ticker===body.ticker);
      Object.assign(result,{nomeEmpresa:a.nome,moeda:a.moeda,cotacaoAtual:a.preco});
    }
    if(route==='/operacoes') route='/carteiras/'+body.carteiraId+'/operacoes';
    if(route.endsWith('/snapshots')) result=snapshot(Number(route.split('/')[2]),getRows(route).length);
    result.id=++next;
    if(route.endsWith('/snapshots')) result.snapshotId=result.id;
    getRows(route).push(result);
    return result;
  }
  const rodar = (save, write = true) => preparar(api,'fixture',state,save,write);
  for(const nome of [' CARTEIRA DE CRESCIMENTO ', 'carteira conservadora', 'Demo — Equilíbrio']){
    rows['/carteiras'].push({id:90,nome});
    await assert.rejects(rodar(()=>{},false),/colide|conciliação/);
    rows['/carteiras'].pop();
  }
  await rodar(()=>{},false);
  assert.equal(writes,0);
  await assert.rejects(rodar(()=>{if(Object.keys(state.ids).length===3 && !state.pendente) throw Error('interrupção após recibo');}));
  assert.equal(writes,3);
  await rodar(()=>{});
  assert.equal(writes,19);
  await rodar(()=>{});
  assert.equal(writes,19);
  rows['/acoes'][0].cotacaoAtual=1;
  await assert.rejects(rodar(()=>{}),/diverge/);
  assert.equal(writes,19);
});

test('cliente usa IPv4 local, Host canônico e CSRF sem seguir redirecionamentos', async t => {
  const http = require('node:http');
  const chamadas = [];
  const server = http.createServer((req,res)=>{
    chamadas.push(req.headers);
    if(req.url==='/api/auth/csrf') res.writeHead(204,{'Set-Cookie':'XSRF-TOKEN=fixture; Secure; SameSite=Lax'});
    else res.writeHead(302,{Location:'/api/inesperado'});
    res.end();
  });
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  t.after(()=>new Promise(resolve=>server.close(resolve)));
  const api=clienteHttp(server.address().port);
  await api('GET','/auth/csrf');
  await assert.rejects(api('POST','/auth/login',{username:'fixture',senha:'ficticia'}),/HTTP 302/);
  assert.equal(chamadas.length,2);
  assert.equal(chamadas[1].host,'localhost:8081');
  assert.equal(chamadas[1].origin,'http://localhost:8081');
  assert.equal(chamadas[1]['x-xsrf-token'],'fixture');
});
