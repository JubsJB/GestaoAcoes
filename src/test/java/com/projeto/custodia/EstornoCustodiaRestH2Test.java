package com.projeto.custodia;

import com.projeto.custodia.support.CustodiaH2IsolationInitializer;
import com.projeto.entities.*;
import com.projeto.mappers.CustodiaMapper;
import com.projeto.resources.MovimentacaoCustodiaResource;
import com.projeto.resources.exceptions.ResourceExceptionHandler;
import com.projeto.services.*;
import com.projeto.repositories.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.atomic.AtomicBoolean;
import java.sql.SQLException;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(value="custodia-h2",inheritProfiles=false)
@ContextConfiguration(classes=EstornoCustodiaRestH2Test.Config.class,initializers=CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class EstornoCustodiaRestH2Test {
    static final OffsetDateTime AGORA=OffsetDateTime.parse("2026-10-24T12:00:00Z");
    static final LocalDate DIA=LocalDate.of(2026,10,1);
    @TestConfiguration(proxyBeanMethods=false)
    @Import({TransferenciaCustodiaTransacaoH2Test.Config.class,ConsultaMovimentacaoCustodiaService.class,CustodiaMapper.class,
            AtribuicaoCustodiaService.class,CadastroCustodiaService.class,CadastroCustodiaResponsePersistenceService.class,com.projeto.services.EstornoCustodiaService.class})
    static class Config { }
    @Autowired ConsultaMovimentacaoCustodiaService consulta;
    @Autowired CadastroCustodiaService cadastro;
    @Autowired PlatformTransactionManager manager;
    @Autowired TransferenciaCustodiaService transferencias;
    @MockitoSpyBean CarteiraRepository carteiras;
    @MockitoSpyBean MovimentacaoCustodiaRepository movimentos;
    @MockitoSpyBean EstornoMovimentacaoCustodiaRepository estornos;
    @MockitoSpyBean OperacaoRepository operacoes;
    @MockitoSpyBean CustodiaMapper mapper;
    @MockitoSpyBean Clock clock;
    @PersistenceContext EntityManager em;
    MockMvc mvc;
    Long carteira,outra,acao,a,b,original;
    <T>T tx(Function<EntityManager,T> trabalho){return new TransactionTemplate(manager).execute(s->trabalho.apply(em));}
    @BeforeEach void preparar(){
        reset(carteiras,movimentos,estornos,operacoes,mapper,clock);
        tx(e->{
            for(String tipo:List.of("EstornoMovimentacaoCustodia","MovimentacaoCustodia","Operacao","Acao","Corretora","Carteira"))e.createQuery("delete from "+tipo).executeUpdate();
            var c=new Carteira("Alvo",AGORA);var o=new Carteira("Outra",AGORA);
            var ac=new Acao("AAPL","Sintética",Mercado.EUA,Moeda.USD,BigDecimal.TEN,AGORA);
            var ca=ConsultaCustodiaRestH2Test.corretora("11111111000111");var cb=ConsultaCustodiaRestH2Test.corretora("22222222000122");
            for(Object item:List.of(c,o,ac,ca,cb))e.persist(item);
            carteira=c.getId();outra=o.getId();acao=ac.getId();a=ca.getId();b=cb.getId();
            e.persist(new Operacao(c,ac,ca,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,1,new BigDecimal("1000")));
            var m=new MovimentacaoCustodia(null,c,ac,NaturezaMovimentacaoCustodia.TRANSFERENCIA,ca,cb,new BigDecimal("40"),DIA,1,PosicaoRacionalCustodia.deTexto("9007199254740993","9007199254740994"),Clock.fixed(AGORA.toInstant(),ZoneOffset.UTC));
            e.persist(m);e.flush();original=m.getId();return null;
        });
        mvc=MockMvcBuilders.standaloneSetup(new MovimentacaoCustodiaResource(consulta,cadastro)).setControllerAdvice(new ResourceExceptionHandler()).build();
    }
    String detalhe(){return "/carteiras/"+carteira+"/movimentacoes-custodia/"+original;}
    org.springframework.test.web.servlet.ResultActions postar(String json)throws Exception{
        var pedido=post(detalhe()+"/estornos");if(json!=null)pedido.contentType("application/json").content(json);
        return mvc.perform(pedido);
    }
    long contarEstornos(){return tx(e->e.createQuery("select count(e) from EstornoMovimentacaoCustodia e",Long.class).getSingleResult());}
    List<List<Object>> retrato(String tabela){return tx(e->{
        @SuppressWarnings("unchecked") List<Object[]> linhas=e.createNativeQuery("select * from "+tabela+" order by id").getResultList();
        return linhas.stream().map(Arrays::asList).toList();
    });}
    void semAcesso(){verifyNoInteractions(carteiras,movimentos,estornos,operacoes);}
    void originalAtiva()throws Exception{
        reset(mapper);
        mvc.perform(get(detalhe())).andExpect(status().isOk()).andExpect(jsonPath("$.movimentacao.situacao").value("ATIVA"))
            .andExpect(jsonPath("$.estorno").value(nullValue()));
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"motivo\":null}","{\"motivo\":\"\"}","{\"motivo\":\" \\t\\n \"}"})
    void variantesSemMotivoNormalizamParaNullEPreservamAuditoria(String json)throws Exception{
        var antes=retrato("operacao");var originais=retrato("movimentacao_custodia");
        postar(json).andExpect(status().isCreated()).andExpect(jsonPath("$.estorno.motivo").value(nullValue()));
        assertEquals(1,contarEstornos());assertEquals(antes,retrato("operacao"));assertEquals(originais,retrato("movimentacao_custodia"));
        tx(e->{assertNull(e.createQuery("from EstornoMovimentacaoCustodia",EstornoMovimentacaoCustodia.class).getSingleResult().getMotivo());return null;});
    }
    @ParameterizedTest @ValueSource(strings={" Correção com espaços ","limite"})
    void textoNaoBrancoInclusiveMilCaracteresPermaneceExato(String caso)throws Exception{
        var motivo=caso.equals("limite")?" x".repeat(500):caso;
        postar(new ObjectMapper().writeValueAsString(Map.of("motivo",motivo))).andExpect(status().isCreated())
            .andExpect(jsonPath("$.estorno.motivo").value(motivo));
        tx(e->{assertEquals(motivo,e.createQuery("from EstornoMovimentacaoCustodia",EstornoMovimentacaoCustodia.class).getSingleResult().getMotivo());return null;});
    }
    @Test void motivoExcedenteMesmoBrancoEhRejeitadoAntesDoBanco()throws Exception{
        postar(new ObjectMapper().writeValueAsString(Map.of("motivo"," ".repeat(1001))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));semAcesso();assertEquals(0,contarEstornos());
    }
    @ParameterizedTest @ValueSource(strings={"id","carteiraId","movimentacaoId","movimentacaoOriginalId","natureza","quantidade","corretoraOrigemId","corretoraDestinoId","dataEfetiva","dataRegistro","posicaoCronologica","situacao","estornoId","precoUnitario","desconhecido"})
    void camposProibidosMesmoNulosNaoSaoIgnorados(String campo)throws Exception{
        postar("{\""+campo+"\":null}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));semAcesso();assertEquals(0,contarEstornos());
    }
    @ParameterizedTest @ValueSource(strings={"true","false","12.3","[]","{}"})
    void tipoDeMotivoInvalidoNaoConsultaBanco(String valor)throws Exception{
        postar("{\"motivo\":"+valor+"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));semAcesso();
    }
    @ParameterizedTest @ValueSource(strings={"[]","123","true","\"texto\"","{\"motivo\":"})
    void corpoDeTipoInvalidoOuMalformadoEh400(String corpo)throws Exception{
        postar(corpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));semAcesso();
    }
    @ParameterizedTest @ValueSource(strings={"carteira","original","fora"})
    void referenciaInexistenteOuForaDaCarteiraRetorna404(String caso)throws Exception{
        Long c=caso.equals("carteira")?Long.MAX_VALUE:caso.equals("fora")?outra:carteira;
        Long m=caso.equals("original")?Long.MAX_VALUE:original;
        mvc.perform(post("/carteiras/"+c+"/movimentacoes-custodia/"+m+"/estornos")).andExpect(status().isNotFound());
        assertEquals(0,contarEstornos());verify(estornos,never()).saveAndFlush(any());
    }
    @ParameterizedTest @ValueSource(strings={"0","-1","texto"})
    void idInvalidoNaoConsultaBanco(String id)throws Exception{
        mvc.perform(post("/carteiras/"+carteira+"/movimentacoes-custodia/"+id+"/estornos")).andExpect(status().isBadRequest());semAcesso();
    }
    @Test void estornoDuplicadoTemPrecedenciaSobreNovoHistoricoInvalido()throws Exception{
        postar(null).andExpect(status().isCreated());
        tx(e->{e.persist(new Operacao(e.find(Carteira.class,carteira),e.find(Acao.class,acao),e.find(Corretora.class,a),TipoOperacao.VENDA,new BigDecimal("101"),BigDecimal.TEN,DIA.plusDays(1),1,new BigDecimal("1010")));return null;});
        clearInvocations(operacoes);
        postar("{}").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ESTORNO_DUPLICADO"));
        verifyNoInteractions(operacoes);assertEquals(1,contarEstornos());verify(estornos,times(1)).saveAndFlush(any());
        mvc.perform(post("/carteiras/"+outra+"/movimentacoes-custodia/"+original+"/estornos")).andExpect(status().isNotFound());
    }
    @Test void estruturaInvalidaPrecedeReferenciaInexistenteEDuplicidade()throws Exception{
        mvc.perform(post("/carteiras/"+Long.MAX_VALUE+"/movimentacoes-custodia/"+original+"/estornos")
            .contentType("application/json").content("{\"quantidade\":1}")).andExpect(status().isBadRequest());semAcesso();
        postar(null).andExpect(status().isCreated());
        postar("{\"motivo\":12}").andExpect(status().isBadRequest());assertEquals(1,contarEstornos());
    }
    void venda(Long corretora,String quantidade){tx(e->{e.persist(new Operacao(e.find(Carteira.class,carteira),e.find(Acao.class,acao),e.find(Corretora.class,corretora),TipoOperacao.VENDA,new BigDecimal(quantidade),BigDecimal.TEN,DIA.plusDays(1),1,new BigDecimal(quantidade).multiply(BigDecimal.TEN)));return null;});}
    @Test void vendaDependenteBloqueiaEstornoComConflitoHistorico()throws Exception{
        venda(b,"30");var antes=retrato("operacao");var originais=retrato("movimentacao_custodia");
        postar(null).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLITO_HISTORICO_QUANTITATIVO"))
            .andExpect(jsonPath("$.details.evento.dominio").value("FINANCEIRO"));
        assertEquals(0,contarEstornos());assertEquals(antes,retrato("operacao"));assertEquals(originais,retrato("movimentacao_custodia"));originalAtiva();
    }
    @Test void consolidadoNegativoPreservaPosicaoInsuficiente()throws Exception{
        venda(a,"101");postar(null).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("POSICAO_INSUFICIENTE"));assertEquals(0,contarEstornos());
    }
    @Test void transferenciaPosteriorDependenteNaoEhEstornadaEmCascata()throws Exception{
        var dependente=transferencias.transferir(new TransferenciaCustodiaCommand(carteira,"AAPL",Mercado.EUA,b,a,new BigDecimal("30"),DIA.plusDays(1)));
        var antes=retrato("movimentacao_custodia");
        postar(null).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MOVIMENTACAO_CUSTODIA_INVALIDADA"))
            .andExpect(jsonPath("$.details.evento.id").value(dependente.getId()));
        assertEquals(0,contarEstornos());assertEquals(antes,retrato("movimentacao_custodia"));
    }
    @Test void estornoDeAtribuicaoRetornaOrigemNulaSemMovimentacaoInversa()throws Exception{
        tx(e->{e.remove(e.find(MovimentacaoCustodia.class,original));e.flush();e.createQuery("delete from Operacao").executeUpdate();
            var c=e.find(Carteira.class,carteira);var ac=e.find(Acao.class,acao);
            e.persist(new Operacao(c,ac,null,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,1,new BigDecimal("1000")));
            var m=new MovimentacaoCustodia(null,c,ac,NaturezaMovimentacaoCustodia.ATRIBUICAO,null,e.find(Corretora.class,b),new BigDecimal("40"),DIA,1,PosicaoRacionalCustodia.deTexto("1","2"),clock);
            e.persist(m);e.flush();original=m.getId();return null;});
        var antes=retrato("operacao");
        postar("{}").andExpect(status().isCreated()).andExpect(jsonPath("$.movimentacao.natureza").value("ATRIBUICAO"))
            .andExpect(jsonPath("$.estorno.corretoraOrigemId").value(nullValue())).andExpect(jsonPath("$.estorno.corretoraDestinoId").value(b));
        assertEquals(1,retrato("movimentacao_custodia").size());assertEquals(antes,retrato("operacao"));
    }
    @Test void consultaDepoisDoEstornoEhConsistenteComRespostaEReplayNeutralizado()throws Exception{
        var antes=retrato("operacao");var originais=retrato("movimentacao_custodia");
        var json=new ObjectMapper().readTree(postar("{\"motivo\":\"Auditoria\"}").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        var detalheAtual=new ObjectMapper().readTree(mvc.perform(get(detalhe())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(json,detalheAtual);assertEquals(17,json.get("movimentacao").size());assertEquals(17,json.get("estorno").size());
        assertEquals(json.get("estorno").get("id"),json.get("movimentacao").get("estornoId"));
        assertEquals("9007199254740993",json.at("/estorno/posicaoCronologica/numerador").asText());
        assertEquals("9007199254740994",json.at("/estorno/posicaoCronologica/denominador").asText());
        assertTrue(json.at("/estorno/situacao").isNull());assertTrue(json.at("/movimentacao/motivo").isNull());
        mvc.perform(get("/carteiras/"+carteira+"/movimentacoes-custodia")).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(2)));
        tx(e->{var fontes=new AdaptadorEventosQuantitativos().adaptar(carteira,acao,operacoes.listarParaReplayQuantitativo(carteira,acao),movimentos.listarPorCarteiraEAcao(carteira,acao),
            List.of(VinculoEstornoConsultado.vinculoPresente(original,estornos.listarPorCarteiraEAcao(carteira,acao).get(0))));
            var eventos=new NormalizadorEventosQuantitativos().normalizar(carteira,acao,fontes.financeiros(),fontes.custodias());
            var estado=new MotorReplayQuantitativo().reproduzir(eventos).estadoFinal();
            assertEquals(0,new BigDecimal("100").compareTo(estado.quantidadeConsolidada()));
            assertEquals(0,new BigDecimal("100").compareTo(estado.saldo(EventoQuantitativo.Grupo.corretora(a))));
            assertEquals(0,BigDecimal.ZERO.compareTo(estado.saldo(EventoQuantitativo.Grupo.corretora(b))));return null;});
        assertEquals(antes,retrato("operacao"));assertEquals(originais,retrato("movimentacao_custodia"));
    }
    @Test void dataRegistroDoEstornoRefletePrecisaoFisicamentePersistida()throws Exception{
        doReturn(Instant.parse("2026-10-24T12:00:00.123456789Z")).when(clock).instant();
        var json=new ObjectMapper().readTree(postar(null).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        var observado=OffsetDateTime.parse(json.at("/estorno/dataRegistro").asText()).toInstant();
        assertEquals(tx(e->e.find(EstornoMovimentacaoCustodia.class,json.at("/estorno/id").asLong()).getDataRegistro().toInstant()),observado);
    }
    @Test void falhaAposFlushReverteSemNovasConsultasNemRetry()throws Exception{
        var antes=retrato("operacao");var originais=retrato("movimentacao_custodia");
        var real=mockingDetails(estornos).getMockCreationSettings().getDefaultAnswer();
        doAnswer(inv->{real.answer(inv);clearInvocations(carteiras,movimentos,operacoes,mapper);throw new DataIntegrityViolationException("SQL senha sentinela");}).when(estornos).saveAndFlush(any());
        var resposta=postar(null).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INTEGRIDADE_DADOS_VIOLADA")).andReturn().getResponse().getContentAsString();
        assertFalse(resposta.contains("sentinela"));verifyNoInteractions(carteiras,movimentos,operacoes,mapper);
        var ordem=inOrder(estornos);ordem.verify(estornos).saveAndFlush(any());ordem.verifyNoMoreInteractions();
        assertEquals(0,contarEstornos());assertEquals(antes,retrato("operacao"));assertEquals(originais,retrato("movimentacao_custodia"));originalAtiva();
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void falhaNoMapperDaOriginalOuEstornoReverteVinculo(boolean naOriginal)throws Exception{
        var antes=retrato("movimentacao_custodia");
        if(naOriginal)doThrow(new IllegalStateException("Montagem interrompida")).when(mapper).original(any(),any());
        else doThrow(new IllegalStateException("Montagem interrompida")).when(mapper).estorno(any());
        assertThrows(jakarta.servlet.ServletException.class,()->postar(null));
        assertEquals(0,contarEstornos());assertEquals(antes,retrato("movimentacao_custodia"));verify(estornos,times(1)).saveAndFlush(any());originalAtiva();
    }
    @Test void vinculoNaoConfirmadoPelaConsultaInterrompeRespostaEReverte()throws Exception{
        doReturn(List.of()).when(estornos).listarPorCarteiraEOriginais(anyLong(),anyCollection());
        assertThrows(jakarta.servlet.ServletException.class,()->postar(null));
        assertEquals(0,contarEstornos());verify(estornos,times(1)).saveAndFlush(any());verifyNoInteractions(mapper);
    }
    @Test void beforeCommitFalhoTraduzConflitoObservavelSemExporCausa()throws Exception{
        var tentouConfirmar=new AtomicBoolean();
        doAnswer(inv->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            var resposta=inv.callRealMethod();TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                @Override public void beforeCommit(boolean readOnly){tentouConfirmar.set(true);throw new RuntimeException("senha sentinela",new SQLException("SQL sentinela","40001"));}
            });return resposta;}).when(mapper).estorno(any());
        var corpo=postar(null).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLITO_TRANSACIONAL_TRANSITORIO")).andReturn().getResponse().getContentAsString();
        assertTrue(tentouConfirmar.get());assertFalse(corpo.contains("sentinela"));assertEquals(0,contarEstornos());verify(estornos,times(1)).saveAndFlush(any());originalAtiva();
    }
    @Test void lockEhPrimeiraConsultaERespostaSoRetornaDepoisDoCommit()throws Exception{
        var confirmou=new AtomicBoolean();var real=mockingDetails(carteiras).getMockCreationSettings().getDefaultAnswer();
        doAnswer(inv->{verifyNoInteractions(movimentos,estornos,operacoes);return real.answer(inv);}).when(carteiras).findByIdForUpdate(carteira);
        doAnswer(inv->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(java.sql.Connection.TRANSACTION_READ_COMMITTED,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            assertFalse(confirmou.get());TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                @Override public void afterCommit(){confirmou.set(true);}
            });return inv.callRealMethod();}).when(mapper).estorno(any());
        postar(null).andExpect(status().isCreated());assertTrue(confirmou.get());verify(estornos,times(1)).saveAndFlush(any());
    }
    @Test void locationRespeitaContextPathEApontaParaOriginal()throws Exception{
        mvc.perform(post("/gestaoacoes"+detalhe()+"/estornos").contextPath("/gestaoacoes"))
            .andExpect(status().isCreated()).andExpect(header().string("Location","http://localhost/gestaoacoes"+detalhe()));
    }
    @Test void motivoNumericoNaoPodeSerCoagidoParaTexto()throws Exception{
        mvc.perform(post(detalhe()+"/estornos").contentType("application/json").content("{\"motivo\":123}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));
    }
    @Test void estornoSemCorpoRetorna201ComVinculoELocationDaOriginal()throws Exception{
        mvc.perform(post(detalhe()+"/estornos")).andExpect(status().isCreated())
            .andExpect(header().string("Location","http://localhost"+detalhe()))
            .andExpect(jsonPath("$.movimentacao.id").value(original))
            .andExpect(jsonPath("$.movimentacao.situacao").value("ESTORNADA"))
            .andExpect(jsonPath("$.estorno.natureza").value("ESTORNO"))
            .andExpect(jsonPath("$.estorno.movimentacaoOriginalId").value(original));
    }
}
