package com.projeto.custodia;

import com.projeto.custodia.support.CustodiaH2IsolationInitializer;
import com.projeto.entities.*;
import com.projeto.resources.*;
import com.projeto.resources.exceptions.ResourceExceptionHandler;
import com.projeto.services.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Somente novas combinações HTTP; regras executadas pelos proxies aprovados. */
@ExtendWith(SpringExtension.class)
@ActiveProfiles(value="custodia-h2",inheritProfiles=false)
@ContextConfiguration(classes=CustodiaPrecedenciaRestH2Test.Config.class,initializers=CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class CustodiaPrecedenciaRestH2Test {
    static final OffsetDateTime AGORA=OffsetDateTime.parse("2026-10-24T12:00:00Z");
    static final LocalDate DIA=LocalDate.of(2026,10,1);
    @TestConfiguration(proxyBeanMethods=false)
    @Import({OperacaoCustodiaTransacaoH2Test.Config.class,CadastroCustodiaRestH2Test.Config.class})
    static class Config { }
    @Autowired OperacaoService operacoes;
    @Autowired ConsultaMovimentacaoCustodiaService consulta;
    @Autowired CadastroCustodiaService cadastro;
    @Autowired TransferenciaCustodiaService transferencias;
    @Autowired EstornoCustodiaService estornos;
    @Autowired PlatformTransactionManager manager;
    @PersistenceContext EntityManager em;
    MockMvc mvc;
    Long carteira,acao,a,b,c;
    <T>T tx(Function<EntityManager,T> f){return new TransactionTemplate(manager).execute(s->f.apply(em));}
    @BeforeEach void preparar(){tx(e->{
        for(String tipo:List.of("EstornoMovimentacaoCustodia","MovimentacaoCustodia","Operacao","Acao","Corretora","Carteira"))e.createQuery("delete from "+tipo).executeUpdate();
        var cart=new Carteira("Precedência",AGORA);var ac=new Acao("PETR4","Sintética",Mercado.BRASIL,Moeda.BRL,BigDecimal.TEN,AGORA);
        var ca=ConsultaCustodiaRestH2Test.corretora("11111111000111");var cb=ConsultaCustodiaRestH2Test.corretora("22222222000122");var cc=ConsultaCustodiaRestH2Test.corretora("33333333000133");
        for(Object item:List.of(cart,ac,ca,cb,cc))e.persist(item);
        carteira=cart.getId();acao=ac.getId();a=ca.getId();b=cb.getId();c=cc.getId();
        e.persist(new Operacao(cart,ac,ca,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,1,new BigDecimal("1000")));e.flush();return null;
    });mvc=MockMvcBuilders.standaloneSetup(new MovimentacaoCustodiaResource(consulta,cadastro),
        new OperacaoResource(operacoes,mock(PrecoOperacaoService.class))).setControllerAdvice(new ResourceExceptionHandler()).build();}
    String rota(){return "/carteiras/"+carteira+"/movimentacoes-custodia";}
    String transferencia(String q,Long destino){return """
        {"natureza":"TRANSFERENCIA","ticker":"PETR4","mercado":"BRASIL","corretoraOrigemId":%d,"corretoraDestinoId":%d,"quantidade":%s,"dataEfetiva":"2026-10-01"}
        """.formatted(a,destino,q);}
    String venda(Long corretora,String q){return """
        {"tipo":"VENDA","carteiraId":%d,"ticker":"PETR4","mercado":"BRASIL","corretoraId":%d,"quantidade":%s,"precoUnitario":10,"dataOperacao":"2026-10-01"}
        """.formatted(carteira,corretora,q);}
    Map<String,List<List<Object>>> estado(){return tx(e->{var resultado=new LinkedHashMap<String,List<List<Object>>>();
        for(String tabela:List.of("operacao","movimentacao_custodia","estorno_movimentacao_custodia")){
            @SuppressWarnings("unchecked") List<Object[]> rows=e.createNativeQuery("select * from "+tabela+" order by id").getResultList();resultado.put(tabela,rows.stream().map(Arrays::asList).toList());
        }return resultado;});}
    void compra(String q){tx(e->{e.createNativeQuery("update operacao set quantidade=?1,valor_total=?2 where carteira_id=?3")
        .setParameter(1,new BigDecimal(q)).setParameter(2,new BigDecimal(q).multiply(BigDecimal.TEN)).setParameter(3,carteira).executeUpdate();return null;});}
    @Test void quantidadeInvalidaPrecedeDestinoInexistente()throws Exception{
        var antes=estado();mvc.perform(post(rota()).contentType("application/json").content(transferencia("0",Long.MAX_VALUE)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));assertEquals(antes,estado());
    }
    @Test void vendaComConsolidadoSuficienteESaldoLocalInsuficiente()throws Exception{
        var antes=estado();mvc.perform(post("/operacoes").contentType("application/json").content(venda(b,"20")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SALDO_CORRETORA_INSUFICIENTE"));assertEquals(antes,estado());
    }
    @Test void vendaConsolidadaInsuficientePrecedeSaldoLocalZero()throws Exception{
        compra("10");var antes=estado();mvc.perform(post("/operacoes").contentType("application/json").content(venda(b,"20")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("POSICAO_INSUFICIENTE"))
            .andExpect(jsonPath("$.details.quantidadeDisponivel").value(10));assertEquals(antes,estado());
    }
    @Test void origemInsuficienteEmTodosEspacosPrecedeCustodiaPosteriorImpeditiva()throws Exception{
        compra("50");tx(e->{e.persist(new Operacao(e.find(Carteira.class,carteira),e.find(Acao.class,acao),null,TipoOperacao.COMPRA,new BigDecimal("50"),BigDecimal.TEN,DIA,2,new BigDecimal("500")));return null;});
        transferencias.transferir(new TransferenciaCustodiaCommand(carteira,"PETR4",Mercado.BRASIL,a,c,new BigDecimal("50"),DIA.plusDays(1)));
        var antes=estado();mvc.perform(post(rota()).contentType("application/json").content(transferencia("60",b)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SALDO_ORIGEM_INSUFICIENTE"));assertEquals(antes,estado());
    }
    @Test void vendaRetroativaInvalidaCustodiaAntesDeClassificarDeficit()throws Exception{
        var t=transferencias.transferir(new TransferenciaCustodiaCommand(carteira,"PETR4",Mercado.BRASIL,a,b,new BigDecimal("80"),DIA.plusDays(1)));
        var antes=estado();mvc.perform(post("/operacoes").contentType("application/json").content(venda(a,"30")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MOVIMENTACAO_CUSTODIA_INVALIDADA"))
            .andExpect(jsonPath("$.details.evento.dominio").value("CUSTODIA"))
            .andExpect(jsonPath("$.details.evento.id").value(t.getId()));assertEquals(antes,estado());
    }
    @Test void quantidadeProibidaPrecedeEstornoDuplicado()throws Exception{
        var t=transferencias.transferir(new TransferenciaCustodiaCommand(carteira,"PETR4",Mercado.BRASIL,a,b,new BigDecimal("20"),DIA));
        estornos.estornar(new EstornoCustodiaCommand(carteira,t.getId(),null));var antes=estado();
        mvc.perform(post(rota()+"/"+t.getId()+"/estornos").contentType("application/json").content("{\"quantidade\":1}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));assertEquals(antes,estado());
    }
}
