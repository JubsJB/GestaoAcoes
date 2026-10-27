package com.projeto.custodia;

import com.projeto.services.CadastroCustodiaService;
import com.projeto.services.CadastroCustodiaResponsePersistenceService;
import com.projeto.services.AtribuicaoCustodiaService;

import com.projeto.custodia.support.CustodiaH2IsolationInitializer;
import com.projeto.entities.*;
import com.projeto.mappers.CustodiaMapper;
import com.projeto.resources.MovimentacaoCustodiaResource;
import com.projeto.resources.exceptions.ResourceExceptionHandler;
import com.projeto.services.ConsultaMovimentacaoCustodiaService;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.sql.Connection;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(value="custodia-h2",inheritProfiles=false)
@ContextConfiguration(classes=ConsultaCustodiaRestH2Test.Config.class,initializers=CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ConsultaCustodiaRestH2Test {
    static final OffsetDateTime AGORA=OffsetDateTime.parse("2026-10-24T12:00:00Z");
    static final LocalDate DIA=LocalDate.of(2026,10,1);
    @TestConfiguration(proxyBeanMethods=false)
    @Import({TransferenciaCustodiaTransacaoH2Test.Config.class,ConsultaMovimentacaoCustodiaService.class,CustodiaMapper.class,AtribuicaoCustodiaService.class,CadastroCustodiaService.class,CadastroCustodiaResponsePersistenceService.class,com.projeto.services.EstornoCustodiaService.class})
    static class Config { }
    @Autowired ConsultaMovimentacaoCustodiaService service;
    @Autowired CadastroCustodiaService cadastro;
    @Autowired PlatformTransactionManager manager;
    @Autowired EntityManagerFactory emf;
    @MockitoSpyBean CustodiaMapper mapper;
    @PersistenceContext EntityManager em;
    MockMvc mvc;
    Long carteira,outra,vazia,acao,a,b,transferencia,atribuicao,estorno;
    <T>T tx(Function<EntityManager,T> trabalho){return new TransactionTemplate(manager).execute(s->trabalho.apply(em));}
    @BeforeEach void preparar(){
        reset(mapper);
        tx(e->{
            for(String tipo:List.of("EstornoMovimentacaoCustodia","MovimentacaoCustodia","Operacao","Acao","Corretora","Carteira"))e.createQuery("delete from "+tipo).executeUpdate();
            var c=new Carteira("Alvo",AGORA);var o=new Carteira("Outra",AGORA);var v=new Carteira("Vazia",AGORA);
            var ac=new Acao("AAPL","Sintética",Mercado.EUA,Moeda.USD,BigDecimal.TEN,AGORA);
            var ca=corretora("11111111000111");var cb=corretora("22222222000122");
            for(Object item:List.of(c,o,v,ac,ca,cb))e.persist(item);
            carteira=c.getId();outra=o.getId();vazia=v.getId();acao=ac.getId();a=ca.getId();b=cb.getId();
            var t=new MovimentacaoCustodia(null,c,ac,NaturezaMovimentacaoCustodia.TRANSFERENCIA,ca,cb,new BigDecimal("40"),DIA,0,PosicaoRacionalCustodia.deTexto("1","2"),clock(AGORA));
            var at=new MovimentacaoCustodia(null,c,ac,NaturezaMovimentacaoCustodia.ATRIBUICAO,null,cb,new BigDecimal("60"),DIA,0,PosicaoRacionalCustodia.deTexto("2","3"),clock(AGORA));
            e.persist(t);e.persist(at);
            var es=new EstornoMovimentacaoCustodia(null,t," motivo preservado ",clock(AGORA));e.persist(es);
            e.persist(new MovimentacaoCustodia(null,o,ac,NaturezaMovimentacaoCustodia.ATRIBUICAO,null,ca,BigDecimal.ONE,DIA,0,PosicaoRacionalCustodia.deTexto("1","2"),clock(AGORA)));
            e.flush();transferencia=t.getId();atribuicao=at.getId();estorno=es.getId();return null;
        });
        mvc=MockMvcBuilders.standaloneSetup(new MovimentacaoCustodiaResource(service,cadastro)).setControllerAdvice(new ResourceExceptionHandler()).build();
    }
    static Clock clock(OffsetDateTime data){return Clock.fixed(data.toInstant(),data.getOffset());}
    static Corretora corretora(String cnpj){return new Corretora(cnpj,"Teste","Nome",null,null,"01001000","Rua","1",null,"Centro","São Paulo","SP","ATIVA",AGORA);}
    String rota(){return "/carteiras/"+carteira+"/movimentacoes-custodia";}
    @Test void historicoCompletoOrdenadoIncluiOriginaisEEstornoSemEscrita()throws Exception{
        mvc.perform(get(rota())).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(atribuicao)).andExpect(jsonPath("$[0].tipoRegistro").value("MOVIMENTACAO"))
                .andExpect(jsonPath("$[1].id").value(transferencia)).andExpect(jsonPath("$[1].situacao").value("ESTORNADA"))
                .andExpect(jsonPath("$[2].id").value(estorno)).andExpect(jsonPath("$[2].tipoRegistro").value("ESTORNO"));
    }
    @ParameterizedTest
    @CsvSource(value={"~,~,A|T|E","~,ATIVA,A","~,ESTORNADA,T",
            "TRANSFERENCIA,~,T","TRANSFERENCIA,ATIVA,NENHUM","TRANSFERENCIA,ESTORNADA,T",
            "ATRIBUICAO,~,A","ATRIBUICAO,ATIVA,A","ATRIBUICAO,ESTORNADA,NENHUM",
            "ESTORNO,~,E","ESTORNO,ATIVA,E","ESTORNO,ESTORNADA,E"},nullValues="~")
    void filtraNaturezaESituacaoComRegraEspecificaParaEstornos(String natureza,String situacao,String esperado)throws Exception{
        var request=get(rota());if(natureza!=null)request.param("natureza",natureza);if(situacao!=null)request.param("situacao",situacao);
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        List<String> atuais=new ArrayList<>();for(var linha:json)atuais.add(linha.get("tipoRegistro").asText()+":"+linha.get("id").asLong());
        List<String> esperados=new ArrayList<>();
        if(!esperado.equals("NENHUM"))for(String token:esperado.split("\\|"))esperados.add(switch(token){
            case "A"->"MOVIMENTACAO:"+atribuicao;case "T"->"MOVIMENTACAO:"+transferencia;case "E"->"ESTORNO:"+estorno;default->throw new AssertionError();});
        assertEquals(esperados,atuais);
    }
    @Test void detalheOriginalEstornadaContemRegistroProprioSemInverterDirecao()throws Exception{
        mvc.perform(get(rota()+"/"+transferencia)).andExpect(status().isOk())
                .andExpect(jsonPath("$.movimentacao.id").value(transferencia))
                .andExpect(jsonPath("$.movimentacao.situacao").value("ESTORNADA"))
                .andExpect(jsonPath("$.movimentacao.estornoId").value(estorno))
                .andExpect(jsonPath("$.estorno.id").value(estorno))
                .andExpect(jsonPath("$.estorno.tipoRegistro").value("ESTORNO"))
                .andExpect(jsonPath("$.estorno.natureza").value("ESTORNO"))
                .andExpect(jsonPath("$.estorno.movimentacaoOriginalId").value(transferencia))
                .andExpect(jsonPath("$.estorno.corretoraOrigemId").value(a))
                .andExpect(jsonPath("$.estorno.corretoraDestinoId").value(b))
                .andExpect(jsonPath("$.estorno.motivo").value(" motivo preservado "));
    }
    @Test void estornoConsultadoTemDataPropriaENulosExplicitos()throws Exception{
        Long id=tx(e->{var es=new EstornoMovimentacaoCustodia(null,e.find(MovimentacaoCustodia.class,atribuicao),null,clock(AGORA.plusSeconds(2)));e.persist(es);e.flush();return es.getId();});
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(mvc.perform(get(rota()+"/"+atribuicao))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var linha=json.get("estorno");assertEquals(17,linha.size());
        assertEquals(id.longValue(),linha.get("id").asLong());
        for(String campo:List.of("situacao","corretoraOrigemId","motivo")){assertTrue(linha.has(campo));assertTrue(linha.get(campo).isNull());}
        assertEquals(AGORA.plusSeconds(2).toInstant(),OffsetDateTime.parse(linha.get("dataRegistro").asText()).toInstant());
        assertEquals("2026-10-01",linha.get("dataEfetiva").asText());
        assertEquals("ESTORNADA",json.get("movimentacao").get("situacao").asText());
    }
    @Test void posicaoFinanceiraEncerradaNaoOcultaAuditoria()throws Exception{
        tx(e->{var c=e.find(Carteira.class,carteira);var ac=e.find(Acao.class,acao);var ca=e.find(Corretora.class,a);var cb=e.find(Corretora.class,b);
            e.persist(new Operacao(c,ac,ca,TipoOperacao.COMPRA,new BigDecimal("40"),BigDecimal.TEN,DIA.minusDays(1),1,new BigDecimal("400")));
            e.persist(new Operacao(c,ac,null,TipoOperacao.COMPRA,new BigDecimal("60"),BigDecimal.TEN,DIA.minusDays(1),2,new BigDecimal("600")));
            e.persist(new Operacao(c,ac,ca,TipoOperacao.VENDA,new BigDecimal("40"),BigDecimal.TEN,DIA.plusDays(1),1,new BigDecimal("400")));
            e.persist(new Operacao(c,ac,cb,TipoOperacao.VENDA,new BigDecimal("60"),BigDecimal.TEN,DIA.plusDays(1),2,new BigDecimal("600")));return null;});
        var antes=retrato();mvc.perform(get(rota())).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(3)));
        assertEquals(antes,retrato());
    }
    @Test void detalheAtivoContemEstornoNuloEOrigemSemCorretora()throws Exception{
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(mvc.perform(get(rota()+"/"+atribuicao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.movimentacao.situacao").value("ATIVA"))
                .andReturn().getResponse().getContentAsString());
        assertTrue(json.has("estorno"));assertTrue(json.get("estorno").isNull());
        var original=json.get("movimentacao");
        for(String campo:List.of("corretoraOrigemId","movimentacaoOriginalId","estornoId","motivo")){
            assertTrue(original.has(campo));assertTrue(original.get(campo).isNull());
        }
        assertEquals(17,original.size());
    }
    @Test void carteiraVaziaAusenteEIsolamentoContextual()throws Exception{
        mvc.perform(get("/carteiras/"+vazia+"/movimentacoes-custodia")).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(0)));
        mvc.perform(get("/carteiras/"+Long.MAX_VALUE+"/movimentacoes-custodia")).andExpect(status().isNotFound());
        mvc.perform(get(rota()+"/"+Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(get("/carteiras/"+outra+"/movimentacoes-custodia/"+transferencia)).andExpect(status().isNotFound());
        mvc.perform(get("/carteiras/"+outra+"/movimentacoes-custodia")).andExpect(status().isOk())
                .andExpect(jsonPath("$",hasSize(1))).andExpect(jsonPath("$[0].carteiraId").value(outra));
    }
    @ParameterizedTest
    @CsvSource({"natureza,transferencia","natureza,ESTORNOS","natureza,' '","natureza,''",
            "situacao,ativa","situacao,INVALIDA","situacao,' '","situacao,''"})
    void filtroInvalidoRetorna400Padronizado(String campo,String valor)throws Exception{
        mvc.perform(get(rota()).param(campo,valor)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALIDO")).andExpect(jsonPath("$.details."+campo).exists());
    }
    @Test void idsInvalidosEMetodosNaoPermitidos()throws Exception{
        mvc.perform(get("/carteiras/0/movimentacoes-custodia")).andExpect(status().isBadRequest());
        mvc.perform(get(rota()+"/0")).andExpect(status().isBadRequest());
        mvc.perform(get(rota()+"/texto")).andExpect(status().isBadRequest());
        mvc.perform(post(rota()).contentType("application/json")).andExpect(status().isBadRequest());
        mvc.perform(delete(rota()+"/"+transferencia)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post(rota()+"/"+transferencia+"/estornos")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ESTORNO_DUPLICADO"));
    }
    @Test void preservaPrecisaoDecimalFracaoTextualEDatasSemCamposFinanceiros()throws Exception{
        Long id=tx(e->{
            var m=new MovimentacaoCustodia(null,e.find(Carteira.class,carteira),e.find(Acao.class,acao),
                    NaturezaMovimentacaoCustodia.ATRIBUICAO,null,e.find(Corretora.class,b),new BigDecimal("9999999999999.123456"),
                    DIA,0,PosicaoRacionalCustodia.deTexto("9007199254740993","9007199254740994"),clock(AGORA.plusSeconds(1)));
            e.persist(m);e.flush();return m.getId();
        });
        var body=mvc.perform(get(rota()+"/"+id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.movimentacao.posicaoCronologica.numerador").value("9007199254740993"))
                .andExpect(jsonPath("$.movimentacao.posicaoCronologica.denominador").value("9007199254740994"))
                .andExpect(jsonPath("$.movimentacao.dataEfetiva").value("2026-10-01"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("\"quantidade\":9999999999999.123456"));
        var m=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("movimentacao");
        assertEquals(AGORA.plusSeconds(1).toInstant(),OffsetDateTime.parse(m.get("dataRegistro").asText()).toInstant());
        for(String campo:List.of("precoUnitario","valorTotal","precoMedio","custo","rentabilidade"))assertFalse(m.has(campo));
    }
    @Test void comparaInstantesAntesDeDesempatarPorTipoOuId()throws Exception{
        Long novo=tx(e->{var m=new MovimentacaoCustodia(null,e.find(Carteira.class,carteira),e.find(Acao.class,acao),
                NaturezaMovimentacaoCustodia.ATRIBUICAO,null,e.find(Corretora.class,a),BigDecimal.ONE,DIA,0,
                PosicaoRacionalCustodia.deTexto("3","4"),clock(AGORA.minusSeconds(1).withOffsetSameInstant(ZoneOffset.ofHours(10))));
            e.persist(m);e.flush();return m.getId();});
        mvc.perform(get(rota())).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(4)))
                .andExpect(jsonPath("$[0].id").value(atribuicao)).andExpect(jsonPath("$[2].tipoRegistro").value("ESTORNO"))
                .andExpect(jsonPath("$[3].id").value(novo));
    }
    @Test void identidadeIncluiTipoRegistroMesmoQuandoIdsCoincidem()throws Exception{
        tx(e->{e.createNativeQuery("update estorno_movimentacao_custodia set id=:id where movimentacao_original_id=:id")
                .setParameter("id",transferencia).executeUpdate();return null;});
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(mvc.perform(get(rota())).andExpect(status().isOk())
                .andExpect(jsonPath("$",hasSize(3))).andReturn().getResponse().getContentAsString());
        assertEquals(transferencia.longValue(),json.get(1).get("id").asLong());
        assertEquals(transferencia.longValue(),json.get(2).get("id").asLong());
        assertNotEquals(json.get(1).get("tipoRegistro"),json.get(2).get("tipoRegistro"));
    }
    @Test void consultaUsaSnapshotProprioReadOnlyMesmoComChamadorMaisFraco(){
        var conexoes=new ArrayList<Integer>();
        doAnswer(inv->{
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_SERIALIZABLE,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            conexoes.add(em.unwrap(Session.class).doReturningWork(c->{assertEquals(Connection.TRANSACTION_SERIALIZABLE,c.getTransactionIsolation());return System.identityHashCode(c);}));
            return inv.callRealMethod();
        }).when(mapper).original(any(),any());
        var externa=new TransactionTemplate(manager);externa.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        externa.execute(s->{
            int anterior=em.unwrap(Session.class).doReturningWork(System::identityHashCode);
            assertEquals(3,service.historico(carteira,null,null).size());
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_READ_COMMITTED,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            assertTrue(conexoes.stream().allMatch(id->id!=anterior));assertEquals(2,conexoes.size());
            assertEquals(anterior,em.unwrap(Session.class).doReturningWork(System::identityHashCode));return null;
        });
    }
    Map<String,List<List<Object>>> retrato(){return tx(e->{
        Map<String,List<List<Object>>> dados=new LinkedHashMap<>();
        for(String tabela:List.of("carteira","acao","corretora","operacao","movimentacao_custodia","estorno_movimentacao_custodia")){
            List<?> linhas=e.createNativeQuery("select * from "+tabela+" order by id").getResultList();
            dados.put(tabela,linhas.stream().map(row->Arrays.asList((Object[])row)).toList());
        }return dados;
    });}
    @Test void historicoRepresentativoSemTruncamentoOuNMaisUmENenhumaEscrita()throws Exception{
        Set<String> esperados=new HashSet<>(List.of("MOVIMENTACAO:"+transferencia,"MOVIMENTACAO:"+atribuicao,"ESTORNO:"+estorno));
        tx(e->{for(int i=0;i<60;i++){
            var corretora=corretora(String.format("%014d",1000+i));e.persist(corretora);
            var m=new MovimentacaoCustodia(null,e.find(Carteira.class,carteira),e.find(Acao.class,acao),
                    NaturezaMovimentacaoCustodia.ATRIBUICAO,null,corretora,BigDecimal.ONE,DIA,0,
                    PosicaoRacionalCustodia.deTexto(Integer.toString(i+3),Integer.toString(i+4)),clock(AGORA.plusSeconds(i+1)));
            e.persist(m);esperados.add("MOVIMENTACAO:"+m.getId());
            if(i%3==0){var es=new EstornoMovimentacaoCustodia(null,m,null,clock(AGORA.plusSeconds(90+i)));e.persist(es);esperados.add("ESTORNO:"+es.getId());}
        }e.flush();return null;});
        var antes=retrato();var stats=emf.unwrap(SessionFactory.class).getStatistics();stats.setStatisticsEnabled(true);stats.clear();
        long inicio=System.nanoTime();
        var resposta=mvc.perform(get(rota())).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(83))).andReturn().getResponse();
        long nanos=System.nanoTime()-inicio;assertEquals(3,stats.getPrepareStatementCount());
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(resposta.getContentAsString());
        Set<String> recebidos=new HashSet<>();for(var row:json)recebidos.add(row.get("tipoRegistro").asText()+":"+row.get("id").asLong());
        assertEquals(esperados,recebidos);stats.clear();
        mvc.perform(get(rota()).param("situacao","ATIVA")).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(41)));
        assertEquals(3,stats.getPrepareStatementCount());stats.clear();
        mvc.perform(get(rota()+"/"+transferencia)).andExpect(status().isOk());assertEquals(3,stats.getPrepareStatementCount());
        assertEquals(0,stats.getEntityInsertCount());assertEquals(0,stats.getEntityUpdateCount());assertEquals(0,stats.getEntityDeleteCount());
        assertEquals(antes,retrato());
        System.out.println("CUSTODIA_CONSULTA registros=83 corretoras=62 consultasPorLeitura=3 payloadBytes="+resposta.getContentAsByteArray().length+" duracaoNanos="+nanos+" escrita=false limiteProduto=false");
    }
}
