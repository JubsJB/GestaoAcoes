package com.projeto.custodia;

import com.projeto.entities.*;
import com.projeto.mappers.CustodiaMapper;
import com.projeto.repositories.*;
import com.projeto.resources.MovimentacaoCustodiaResource;
import com.projeto.resources.exceptions.ResourceExceptionHandler;
import com.projeto.services.*;
import com.projeto.custodia.support.CustodiaH2IsolationInitializer;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.IllegalTransactionStateException;
import com.projeto.services.exceptions.ApiException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import com.fasterxml.jackson.databind.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(value="custodia-h2",inheritProfiles=false)
@ContextConfiguration(classes=CadastroCustodiaRestH2Test.Config.class,initializers=CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class CadastroCustodiaRestH2Test {
    static final OffsetDateTime AGORA=OffsetDateTime.parse("2026-10-24T12:00:00Z");
    static final LocalDate DIA=LocalDate.of(2026,10,1);
    @TestConfiguration(proxyBeanMethods=false)
    @Import({TransferenciaCustodiaTransacaoH2Test.Config.class,ConsultaMovimentacaoCustodiaService.class,CustodiaMapper.class,AtribuicaoCustodiaService.class,CadastroCustodiaService.class,CadastroCustodiaResponsePersistenceService.class,com.projeto.services.EstornoCustodiaService.class})
    static class Config { }
    @Autowired ConsultaMovimentacaoCustodiaService consulta;
    @Autowired CadastroCustodiaService cadastro;
    @MockitoSpyBean CarteiraRepository carteiras;
    @MockitoSpyBean AcaoRepository acoes;
    @MockitoSpyBean CorretoraRepository corretoras;
    @MockitoSpyBean OperacaoRepository operacoes;
    @MockitoSpyBean MovimentacaoCustodiaRepository registros;
    @MockitoSpyBean EstornoMovimentacaoCustodiaRepository estornos;
    @MockitoSpyBean CustodiaMapper mapper;
    @MockitoSpyBean Clock clock;
    @Autowired PlatformTransactionManager manager;
    @PersistenceContext EntityManager em;
    MockMvc mvc;
    Long carteira,outra,acao,a,b;
    <T>T tx(Function<EntityManager,T> trabalho){return new TransactionTemplate(manager).execute(s->trabalho.apply(em));}
    @BeforeEach void preparar(){
        reset(carteiras,acoes,corretoras,operacoes,registros,estornos,mapper,clock);
        tx(e->{
            for(String tipo:List.of("EstornoMovimentacaoCustodia","MovimentacaoCustodia","Operacao","Acao","Corretora","Carteira"))e.createQuery("delete from "+tipo).executeUpdate();
            var c=new Carteira("Alvo",AGORA);var o=new Carteira("Outra",AGORA);
            var ac=new Acao("AAPL","Sintética",Mercado.EUA,Moeda.USD,BigDecimal.TEN,AGORA);
            var ca=ConsultaCustodiaRestH2Test.corretora("11111111000111");var cb=ConsultaCustodiaRestH2Test.corretora("22222222000122");
            for(Object item:List.of(c,o,ac,ca,cb))e.persist(item);
            carteira=c.getId();outra=o.getId();acao=ac.getId();a=ca.getId();b=cb.getId();
            e.persist(new Operacao(c,ac,ca,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,1,new BigDecimal("1000")));
            e.persist(new Operacao(c,ac,null,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,2,new BigDecimal("1000")));
            e.flush();return null;
        });
        mvc=MockMvcBuilders.standaloneSetup(new MovimentacaoCustodiaResource(consulta,cadastro)).setControllerAdvice(new ResourceExceptionHandler()).build();
        clearInvocations(carteiras,acoes,corretoras,operacoes,registros,estornos);
    }
    String rota(){return "/carteiras/"+carteira+"/movimentacoes-custodia";}
    String entrada(String natureza,String origem,String quantidade){return """
        {"natureza":"%s","ticker":"AAPL","mercado":"EUA",%s"corretoraDestinoId":%d,"quantidade":%s,"dataEfetiva":"2026-10-01"}
        """.formatted(natureza,origem,b,quantidade);}
    long movimentos(){return tx(e->e.createQuery("select count(m) from MovimentacaoCustodia m",Long.class).getSingleResult());}
    List<List<Object>> financeiro(){return tx(e->{
        @SuppressWarnings("unchecked") List<Object[]> linhas=e.createNativeQuery("select id,carteira_id,acao_id,corretora_id,tipo,quantidade,preco_unitario,valor_total,data_operacao,ordem_no_dia from operacao order by id").getResultList();
        return linhas.stream().map(Arrays::asList).toList();
    });}
    String transferencia(String quantidade){return entrada("TRANSFERENCIA","\"corretoraOrigemId\":"+a+",",quantidade);}
    @Test void dataRegistroDaRespostaCorrespondeAoValorFisicamentePersistido()throws Exception{
        doReturn(Instant.parse("2026-10-24T12:00:00.123456789Z")).when(clock).instant();
        var resposta=postar(transferencia("40")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var json=new ObjectMapper().readTree(resposta);
        var instante=OffsetDateTime.parse(json.get("dataRegistro").asText()).toInstant();
        var persistido=tx(e->e.find(MovimentacaoCustodia.class,json.get("id").asLong()).getDataRegistro().toInstant());
        assertEquals(persistido,instante);
    }
    org.springframework.test.web.servlet.ResultActions postar(String json)throws Exception{
        return mvc.perform(post(rota()).contentType("application/json").content(json));
    }
    void nenhumAcesso(){verifyNoInteractions(carteiras,acoes,corretoras,operacoes,registros,estornos);}
    @ParameterizedTest @ValueSource(strings={"","\"corretoraOrigemId\":null,"})
    void atribuicaoAceitaOrigemOmitidaOuNulaEPreservaCompra(String origem)throws Exception{
        var antes=financeiro();
        var resultado=postar(entrada("ATRIBUICAO",origem,"40")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.natureza").value("ATRIBUICAO"))
                .andExpect(jsonPath("$.corretoraOrigemId").value(nullValue()))
                .andExpect(jsonPath("$.estornoId").value(nullValue()))
                .andExpect(jsonPath("$.movimentacaoOriginalId").value(nullValue()))
                .andExpect(jsonPath("$.motivo").value(nullValue())).andReturn().getResponse();
        assertNotNull(resultado.getHeader("Location"));assertEquals(antes,financeiro());assertEquals(1,movimentos());
        tx(e->{var m=e.createQuery("from MovimentacaoCustodia",MovimentacaoCustodia.class).getSingleResult();assertNull(m.getCorretoraOrigemId());assertEquals(b,m.getCorretoraDestinoId());return null;});
    }
    @ParameterizedTest @ValueSource(strings={"\"ESTORNO\"","\"COMPRA\"","\"transferencia\"","null","3","\"\""})
    void naturezaInvalidaNaoConsultaBanco(String natureza)throws Exception{
        postar(transferencia("40").replace("\"natureza\":\"TRANSFERENCIA\"","\"natureza\":"+natureza))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));nenhumAcesso();
    }
    @Test void naturezaAusenteNaoConsultaBanco()throws Exception{
        postar(transferencia("40").replace("\"natureza\":\"TRANSFERENCIA\",",""))
            .andExpect(status().isBadRequest());nenhumAcesso();
    }
    @ParameterizedTest @ValueSource(strings={"id","carteiraId","acaoId","preco","precoUnitario","valorTotal","posicaoCronologica","ordemFinanceiraAncora","numerador","denominador","dataRegistro","situacao","tipoRegistro","movimentacaoOriginalId","estornoId","motivo","desconhecido"})
    void camposProibidosMesmoNulosNaoSaoDescartados(String campo)throws Exception{
        postar(transferencia("40").replaceFirst("\\{","{\""+campo+"\":null,"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));nenhumAcesso();
    }
    @ParameterizedTest @CsvSource({"EUA,0","EUA,-1","EUA,0.0000001","EUA,10000000000000","BRASIL,1.5","EUA,null"})
    void quantidadeInvalidaNaoAcessaBanco(String mercado,String quantidade)throws Exception{
        postar(transferencia(quantidade).replace("\"EUA\"","\""+mercado+"\""))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));nenhumAcesso();
    }
    @ParameterizedTest @ValueSource(strings={"","\"corretoraOrigemId\":null,","\"corretoraOrigemId\":0,"})
    void transferenciaExigeOrigemPositiva(String origem)throws Exception{
        postar(entrada("TRANSFERENCIA",origem,"40")).andExpect(status().isBadRequest());nenhumAcesso();
    }
    @Test void origemIgualDestinoEOrigemNaAtribuicaoSaoRejeitadasAntesDoLock()throws Exception{
        postar(entrada("TRANSFERENCIA","\"corretoraOrigemId\":"+b+",","40")).andExpect(status().isBadRequest());
        postar(entrada("ATRIBUICAO","\"corretoraOrigemId\":"+a+",","40")).andExpect(status().isBadRequest());nenhumAcesso();
    }
    @Test void dataFuturaTemCodigoProprioAntesDeReferenciaAusente()throws Exception{
        postar(transferencia("40").replace("2026-10-01","2026-10-25").replace("\"AAPL\"","\"AUSENTE\""))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATA_EFETIVA_FUTURA"));nenhumAcesso();
    }
    @ParameterizedTest @ValueSource(strings={"carteira","acao","origem","destino"})
    void referenciaAusenteRetorna404SemEscrita(String campo)throws Exception{
        String body=transferencia("40");String path=rota();
        switch(campo){
            case "carteira"->path="/carteiras/"+Long.MAX_VALUE+"/movimentacoes-custodia";
            case "acao"->body=body.replace("AAPL","AUSENTE");
            case "origem"->body=body.replace("\"corretoraOrigemId\":"+a, "\"corretoraOrigemId\":"+Long.MAX_VALUE);
            case "destino"->body=body.replace("\"corretoraDestinoId\":"+b, "\"corretoraDestinoId\":"+Long.MAX_VALUE);
        }
        mvc.perform(post(path).contentType("application/json").content(body)).andExpect(status().isNotFound());
        verify(registros,never()).saveAndFlush(any());assertEquals(0,movimentos());
    }
    @ParameterizedTest @ValueSource(strings={"0.000001","99.123456","\"0.000001\"","\"99.123456\""})
    void decimalJsonOuStringPreservadoNaRespostaENoBanco(String quantidade)throws Exception{
        var antes=financeiro();
        var body=postar(transferencia(quantidade)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var json=new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(body);
        var esperado=new BigDecimal(quantidade.replace("\"",""));
        assertEquals(0,esperado.compareTo(json.get("quantidade").decimalValue()));assertTrue(json.get("quantidade").isNumber());
        tx(e->{assertEquals(0,esperado.compareTo(e.find(MovimentacaoCustodia.class,json.get("id").asLong()).getQuantidade()));return null;});
        assertEquals(antes,financeiro());
    }
    @Test void quantidadeGrandeExataNaoPassaPorDouble()throws Exception{
        tx(e->{var op=e.createQuery("from Operacao where corretora is not null",Operacao.class).getSingleResult();e.remove(op);e.flush();
            e.persist(new Operacao(e.find(Carteira.class,carteira),e.find(Acao.class,acao),e.find(Corretora.class,a),TipoOperacao.COMPRA,new BigDecimal("9999999999999.999999"),new BigDecimal("0.000001"),DIA,1,new BigDecimal("9999999.999999999999")));return null;});
        var resposta=postar(transferencia("9999999999999.999999")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertTrue(resposta.contains("9999999999999.999999"));
    }
    @Test void lockPrimeiroEMontagemAntesDoCommitSobMesmaTransacao()throws Exception{
        var confirmado=new AtomicBoolean();
        var real=mockingDetails(carteiras).getMockCreationSettings().getDefaultAnswer();
        doAnswer(inv->{verifyNoInteractions(acoes,corretoras,operacoes,registros,estornos);return real.answer(inv);})
                .when(carteiras).findByIdForUpdate(carteira);
        doAnswer(inv->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(java.sql.Connection.TRANSACTION_READ_COMMITTED,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){confirmado.set(true);}});
            assertFalse(confirmado.get());return inv.callRealMethod();}).when(mapper).original(any(),any());
        postar(transferencia("40")).andExpect(status().isCreated());assertTrue(confirmado.get());
        var ordem=inOrder(carteiras,acoes,corretoras,operacoes,registros,estornos);
        ordem.verify(carteiras).findByIdForUpdate(carteira);
        verify(registros,times(1)).saveAndFlush(any());
        reset(carteiras);
    }
    @Test void insuficienciaLocalEIsolamentoPreservamCodigoSemPersistencia()throws Exception{
        postar(transferencia("101")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SALDO_ORIGEM_INSUFICIENTE"));
        mvc.perform(post("/carteiras/"+outra+"/movimentacoes-custodia").contentType("application/json").content(transferencia("1")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SALDO_ORIGEM_INSUFICIENTE"));
        assertEquals(0,movimentos());
    }
    @Test void conflitoHistoricoPreservaCodigoExternoECausaEspecifica()throws Exception{
        tx(e->{e.persist(new Operacao(e.find(Carteira.class,carteira),e.find(Acao.class,acao),e.find(Corretora.class,a),TipoOperacao.VENDA,new BigDecimal("80"),BigDecimal.TEN,DIA.plusDays(1),1,new BigDecimal("800")));return null;});
        var antes=financeiro();
        postar(transferencia("50")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("POSICAO_CUSTODIA_INDISPONIVEL"))
            .andExpect(jsonPath("$.details.motivo").value("CONFLITO_HISTORICO_QUANTITATIVO"));
        assertEquals(0,movimentos());assertEquals(antes,financeiro());
    }
    @Test void custodiaPosteriorInvalidadaPreservaIdentidadeECausaNoErroHttp()throws Exception{
        Long original=tx(e->{var m=new MovimentacaoCustodia(null,e.find(Carteira.class,carteira),e.find(Acao.class,acao),NaturezaMovimentacaoCustodia.TRANSFERENCIA,
            e.find(Corretora.class,a),e.find(Corretora.class,b),new BigDecimal("80"),DIA.plusDays(1),0,PosicaoRacionalCustodia.deTexto("1","2"),clock);e.persist(m);e.flush();return m.getId();});
        postar(transferencia("30")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("POSICAO_CUSTODIA_INDISPONIVEL"))
            .andExpect(jsonPath("$.details.motivo").value("MOVIMENTACAO_CUSTODIA_INVALIDADA"))
            .andExpect(jsonPath("$.details.evento.dominio").value("CUSTODIA"))
            .andExpect(jsonPath("$.details.evento.id").value(original));
        assertEquals(1,movimentos());verify(registros,never()).saveAndFlush(any());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void flushAceitoSeguidoDeFalhaReverteSemConsultaAdicional(boolean atribuicao)throws Exception{
        var antes=financeiro();var real=mockingDetails(registros).getMockCreationSettings().getDefaultAnswer();
        doAnswer(inv->{real.answer(inv);clearInvocations(carteiras,acoes,corretoras,operacoes,estornos,mapper);throw new DataIntegrityViolationException("Injetada após flush");}).when(registros).saveAndFlush(any());
        postar(atribuicao?entrada("ATRIBUICAO","","40"):transferencia("40")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INTEGRIDADE_DADOS_VIOLADA"));
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,estornos,mapper);verify(registros,times(1)).saveAndFlush(any());
        assertEquals(0,movimentos());assertEquals(antes,financeiro());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void falhaDuranteMontagemAindaReverteTodaAEscrita(boolean atribuicao)throws Exception{
        var antes=financeiro();doThrow(new IllegalStateException("Falha de montagem")).when(mapper).original(any(),any());
        assertThrows(jakarta.servlet.ServletException.class,()->postar(atribuicao?entrada("ATRIBUICAO","","40"):transferencia("40")));
        assertEquals(0,movimentos());assertEquals(antes,financeiro());verify(registros,times(1)).saveAndFlush(any());
    }
    @Test void consultaDeVinculoFalhaAntesDoCommitENaoDeixaCadastroConfirmado()throws Exception{
        var antes=financeiro();doThrow(new IllegalStateException("Consulta inconclusiva")).when(estornos).listarPorCarteiraEOriginais(anyLong(),anyCollection());
        assertThrows(jakarta.servlet.ServletException.class,()->postar(transferencia("40")));
        verify(registros,times(1)).saveAndFlush(any());verifyNoInteractions(mapper);
        assertEquals(0,movimentos());assertEquals(antes,financeiro());
    }
    @Test void consolidadoInvalidoTemPrecedenciaSobreSaldoLocal()throws Exception{
        tx(e->{e.persist(new Operacao(e.find(Carteira.class,carteira),e.find(Acao.class,acao),e.find(Corretora.class,a),TipoOperacao.VENDA,new BigDecimal("210"),BigDecimal.TEN,DIA.plusDays(1),1,new BigDecimal("2100")));return null;});
        postar(transferencia("101")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("POSICAO_INSUFICIENTE"));
        assertEquals(0,movimentos());
    }
    @Test void referenciasTemPrecedenciaSobreInsuficiencia()throws Exception{
        postar(transferencia("101").replace("\"corretoraDestinoId\":"+b,"\"corretoraDestinoId\":"+Long.MAX_VALUE)).andExpect(status().isNotFound());
        verifyNoInteractions(operacoes);assertEquals(0,movimentos());
    }
    @Test void atribuicaoDestinoAusenteRetorna404()throws Exception{
        postar(entrada("ATRIBUICAO","","40").replace("\"corretoraDestinoId\":"+b,"\"corretoraDestinoId\":"+Long.MAX_VALUE)).andExpect(status().isNotFound());
        assertEquals(0,movimentos());
    }
    @ParameterizedTest @ValueSource(strings={"TRANSFERENCIA","ATRIBUICAO"})
    void brasilAceitaInteiroComSeisCasasSemAlterarCompra(String natureza)throws Exception{
        tx(e->{var ac=new Acao("PETR4","Sintética",Mercado.BRASIL,Moeda.BRL,BigDecimal.TEN,AGORA);e.persist(ac);
            e.persist(new Operacao(e.find(Carteira.class,carteira),ac,natureza.equals("TRANSFERENCIA")?e.find(Corretora.class,a):null,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,3,new BigDecimal("1000")));return null;});
        var antes=financeiro();
        postar(entrada(natureza,natureza.equals("TRANSFERENCIA")?"\"corretoraOrigemId\":"+a+",":"","100.000000").replace("AAPL","PETR4").replace("EUA","BRASIL"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.quantidade").value(100));
        assertEquals(antes,financeiro());assertEquals(1,movimentos());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void falhaBeforeCommitObservadaPelaFachadaTraduz409EReverte(boolean atribuicao)throws Exception{
        var antes=financeiro();var encerrou=new AtomicBoolean();
        doAnswer(inv->{var resposta=inv.callRealMethod();TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void beforeCommit(boolean readOnly){encerrou.set(true);throw new RuntimeException("Falha controlada",new SQLException("Não expor","40001"));}
        });return resposta;}).when(mapper).original(any(),any());
        postar(atribuicao?entrada("ATRIBUICAO","","40"):transferencia("40")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLITO_TRANSACIONAL_TRANSITORIO"));
        assertTrue(encerrou.get());assertEquals(0,movimentos());assertEquals(antes,financeiro());verify(registros,times(1)).saveAndFlush(any());
    }
    @Test void transacaoExternaValidaParticipaEMantemRollbackDoChamador(){
        var template=new TransactionTemplate(manager);template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.execute(s->{assertNotNull(cadastro.transferir(new TransferenciaCustodiaCommand(carteira,"AAPL",Mercado.EUA,a,b,BigDecimal.ONE,DIA)).id());s.setRollbackOnly();return null;});
        assertEquals(0,movimentos());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void contextosExternosIncompativeisContinuamSemConsultas(boolean readOnly){
        var template=new TransactionTemplate(manager);template.setReadOnly(readOnly);
        template.setIsolationLevel(readOnly?TransactionDefinition.ISOLATION_READ_COMMITTED:TransactionDefinition.ISOLATION_SERIALIZABLE);
        assertThrows(IllegalTransactionStateException.class,()->template.execute(s->cadastro.transferir(new TransferenciaCustodiaCommand(carteira,"AAPL",Mercado.EUA,a,b,BigDecimal.ONE,DIA))));
        nenhumAcesso();
    }
    @Test void discriminadorContraditorioNaoPodeSelecionarUmaNaturezaEPersistirOutra()throws Exception{
        String json=entrada("ATRIBUICAO","","40").replace("\"ticker\"","\"natureza\":\"TRANSFERENCIA\",\"ticker\"");
        mvc.perform(post(rota()).contentType("application/json").content(json)).andExpect(status().isBadRequest());
        assertEquals(0,movimentos());
    }
    @Test void transferenciaRetornaOriginalPersistidaELocationDeDetalhe()throws Exception{
        var resposta=mvc.perform(post(rota()).contentType("application/json").content(entrada("TRANSFERENCIA","\"corretoraOrigemId\":"+a+",","40")))
            .andExpect(status().isCreated()).andExpect(header().string("Location",containsString(rota()+"/")))
            .andExpect(jsonPath("$.natureza").value("TRANSFERENCIA"))
            .andExpect(jsonPath("$.tipoRegistro").value("MOVIMENTACAO"))
            .andExpect(jsonPath("$.situacao").value("ATIVA"))
            .andExpect(jsonPath("$.carteiraId").value(carteira))
            .andExpect(jsonPath("$.corretoraOrigemId").value(a))
            .andExpect(jsonPath("$.corretoraDestinoId").value(b))
            .andExpect(jsonPath("$.quantidade").value(40))
            .andExpect(jsonPath("$.posicaoCronologica.numerador").isString()).andReturn().getResponse();
        var json=new ObjectMapper().readTree(resposta.getContentAsString());
        assertEquals(17,json.size());
        assertEquals("http://localhost"+rota()+"/"+json.get("id").asLong(),resposta.getHeader("Location"));
        mvc.perform(get(resposta.getHeader("Location"))).andExpect(status().isOk()).andExpect(jsonPath("$.movimentacao.quantidade").value(40));
        assertEquals(1,movimentos());
    }
}
