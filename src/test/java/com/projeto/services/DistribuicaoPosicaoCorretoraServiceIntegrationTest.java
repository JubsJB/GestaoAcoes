package com.projeto.services;

import com.projeto.entities.*;
import com.projeto.services.exceptions.ObjectNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.resources.DistribuicaoCorretoraResource;
import com.projeto.resources.exceptions.ResourceExceptionHandler;
import com.projeto.mappers.DistribuicaoCorretoraMapper;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.hibernate.Session;
import java.sql.Connection;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(classes = DistribuicaoPosicaoCorretoraServiceIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:broker-distribution;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
class DistribuicaoPosicaoCorretoraServiceIntegrationTest {
    @TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.projeto.entities")
    @EnableJpaRepositories("com.projeto.repositories")
    @Import({DistribuicaoPosicaoCorretoraService.class, CalculadoraDistribuicaoCorretora.class,
            CalculadoraPosicao.class})
    static class Config { }
    @Autowired DistribuicaoPosicaoCorretoraService service;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean CarteiraRepository carteiras;

    @Test void getRepetidoComPersistenciaRealNaoEscreveNemCarregaEntidadesAposTransacao() throws Exception {
        Long[] ids = new TransactionTemplate(transactions).execute(s -> {
            Carteira c = carteira(); em.persist(c);
            Acao a = new Acao("MSFT", "Microsoft", Mercado.EUA, Moeda.USD,
                    BigDecimal.TEN, OffsetDateTime.now()); em.persist(a);
            op(c, a, null, TipoOperacao.COMPRA, "0.123456", 1);
            return new Long[]{c.getId(), a.getId()};
        });
        var mvc = MockMvcBuilders.standaloneSetup(new DistribuicaoCorretoraResource(service,
                        new DistribuicaoCorretoraMapper()))
                .setControllerAdvice(new ResourceExceptionHandler()).build();
        var stats = emf.unwrap(SessionFactory.class).getStatistics(); stats.clear();
        String path = "/carteiras/" + ids[0] + "/posicoes/distribuicao-corretoras";
        String primeiro = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        String segundo = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(primeiro, segundo);
        assertTrue(primeiro.contains("\"quantidadeConsolidada\":0.123456"));
        assertTrue(primeiro.contains("Sem corretora informada"));
        assertEquals(8, stats.getPrepareStatementCount()); // duas leituras, quatro consultas completas por snapshot
        assertEquals(0, stats.getEntityInsertCount());
        assertEquals(0, stats.getEntityUpdateCount());
        assertEquals(0, stats.getEntityDeleteCount());
        new TransactionTemplate(transactions).executeWithoutResult(s -> {
            assertEquals(0, BigDecimal.TEN.compareTo(em.find(Acao.class, ids[1]).getCotacaoAtual()));
            var operacoes = em.createQuery("select o from Operacao o where o.carteira.id = :id", Operacao.class)
                    .setParameter("id", ids[0]).getResultList();
            assertEquals(1, operacoes.size());
            assertEquals(0, new BigDecimal("0.123456").compareTo(operacoes.get(0).getQuantidade()));
            assertEquals(0, BigDecimal.TEN.compareTo(operacoes.get(0).getPrecoUnitario()));
        });
    }

    @Test void isolamentoEfetivoMesmoComChamadorMaisFraco() {
        Long id = new TransactionTemplate(transactions).execute(s -> {
            Carteira c = carteira(); em.persist(c); return c.getId();
        });
        doAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_SERIALIZABLE,
                    em.unwrap(Session.class).doReturningWork(Connection::getTransactionIsolation));
            return java.util.Optional.ofNullable(em.find(Carteira.class, id));
        }).when(carteiras).findById(id);
        TransactionTemplate caller = new TransactionTemplate(transactions);
        caller.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        caller.executeWithoutResult(s -> {
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertTrue(service.consultar(id).posicoes().isEmpty());
            assertEquals(Connection.TRANSACTION_READ_COMMITTED,
                    em.unwrap(Session.class).doReturningWork(Connection::getTransactionIsolation));
        });
    }

    @Test void inexistenteNaoConsultaOperacoes() {
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        assertThrows(ObjectNotFoundException.class, () -> service.consultar(Long.MAX_VALUE));
        assertEquals(1, stats.getPrepareStatementCount());
    }

    @Test void carteiraVazia() {
        Long id = new TransactionTemplate(transactions).execute(s -> {
            Carteira c = carteira(); em.persist(c); return c.getId();
        });
        assertTrue(service.consultar(id).posicoes().isEmpty());
    }

    @Test void posicaoEncerradaNaoRetornaMesmoComDeficitHistorico() {
        Long id = new TransactionTemplate(transactions).execute(s -> {
            Carteira c = carteira(); em.persist(c);
            Acao a = new Acao("VALE3", "Vale", Mercado.BRASIL, Moeda.BRL,
                    BigDecimal.TEN, OffsetDateTime.now()); em.persist(a);
            Corretora b = broker("00000000000004", "Encerrada"); em.persist(b);
            op(c, a, null, TipoOperacao.COMPRA, "10", 1);
            op(c, a, b, TipoOperacao.VENDA, "10", 2);
            return c.getId();
        });
        assertTrue(service.consultar(id).posicoes().isEmpty());
    }

    @Test void numeroDeConsultasNaoCresceComQuantidadeDeCorretoras() {
        Long id = new TransactionTemplate(transactions).execute(s -> {
            Carteira c = carteira(); em.persist(c);
            Acao a = new Acao("ITUB4", "Itau", Mercado.BRASIL, Moeda.BRL,
                    BigDecimal.TEN, OffsetDateTime.now()); em.persist(a);
            for (int i = 0; i < 12; i++) {
                Corretora b = broker(String.format("%014d", 100 + i), i % 2 == 0 ? "a" : "A");
                em.persist(b); op(c, a, b, TipoOperacao.COMPRA, "1", i + 1);
            }
            return c.getId();
        });
        var stats = emf.unwrap(SessionFactory.class).getStatistics(); stats.clear();
        var p = service.consultar(id).posicoes().get(0);
        assertEquals(12, p.corretoras().size());
        assertEquals("A", p.corretoras().get(0).nome());
        assertEquals("a", p.corretoras().get(11).nome());
        for (int i = 1; i < 6; i++) {
            assertTrue(p.corretoras().get(i - 1).corretoraId() < p.corretoras().get(i).corretoraId());
        }
        assertEquals(4, stats.getPrepareStatementCount()); // carteira, operações, originais e vínculos em lote
    }

    @Test void snapshotMantemOperacoesENomesQuandoOutraTransacaoEscreve() {
        Long[] ids = new TransactionTemplate(transactions).execute(s -> {
            Carteira c = carteira(); em.persist(c);
            Acao a = new Acao("AAPL", "Apple", Mercado.EUA, Moeda.USD,
                    BigDecimal.TEN, OffsetDateTime.now()); em.persist(a);
            Corretora b = broker("00000000000003", "Antes"); em.persist(b);
            op(c, a, b, TipoOperacao.COMPRA, "0.123456", 1);
            return new Long[]{c.getId(), a.getId(), b.getId()};
        });
        doAnswer(invocation -> {
            Object carteira = java.util.Optional.ofNullable(em.find(Carteira.class, ids[0]));
            TransactionTemplate writer = new TransactionTemplate(transactions);
            writer.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            writer.executeWithoutResult(s -> {
                em.createQuery("update Corretora b set b.nomeFantasia = 'Depois' where b.id = :id")
                        .setParameter("id", ids[2]).executeUpdate();
                op(em.find(Carteira.class, ids[0]), em.find(Acao.class, ids[1]),
                        em.find(Corretora.class, ids[2]), TipoOperacao.COMPRA, "0.000001", 2);
            });
            return carteira;
        }).when(carteiras).findById(ids[0]);
        var p = service.consultar(ids[0]).posicoes().get(0);
        assertEquals(0, new BigDecimal("0.123456").compareTo(p.quantidadeConsolidada()));
        assertEquals("Antes", p.corretoras().get(0).nome());
        assertTrue(p.reconciliada());
        reset(carteiras);
        var novo = service.consultar(ids[0]).posicoes().get(0);
        assertEquals(0, new BigDecimal("0.123457").compareTo(novo.quantidadeConsolidada()));
        assertEquals("Depois", novo.corretoras().get(0).nome());
    }

    @Test void fetchUnicoPreservaZeroHistoricoNegativosNullEIsolamentoSemEscritas() {
        Long id = new TransactionTemplate(transactions).execute(s -> {
            Carteira c = carteira(); em.persist(c);
            Carteira outra = carteira(); em.persist(outra);
            Acao a = new Acao("PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL,
                    BigDecimal.TEN, OffsetDateTime.now()); em.persist(a);
            Corretora b = broker("00000000000001", "B"); em.persist(b);
            Corretora z = broker("00000000000002", "Z"); em.persist(z);
            op(c, a, b, TipoOperacao.COMPRA, "100", 1);
            op(c, a, z, TipoOperacao.VENDA, "10", 2);
            op(c, a, z, TipoOperacao.COMPRA, "10", 3);
            op(c, a, null, TipoOperacao.VENDA, "5", 4);
            op(outra, a, b, TipoOperacao.COMPRA, "999", 1);
            return c.getId();
        });
        var stats = emf.unwrap(SessionFactory.class).getStatistics(); stats.clear();
        var result = service.consultar(id);
        var p = result.posicoes().get(0);
        assertEquals(1, result.posicoes().size());
        assertEquals(0, new BigDecimal("95").compareTo(p.quantidadeConsolidada()));
        assertEquals(0, p.quantidadeConsolidada().compareTo(p.quantidadeAtribuidaTotal()));
        assertTrue(p.reconciliada());
        assertEquals(2, p.corretoras().size());
        assertEquals("B", p.corretoras().get(0).nome());
        assertNull(p.corretoras().get(1).corretoraId());
        assertEquals("Sem corretora informada", p.corretoras().get(1).nome());
        assertEquals(3, p.diagnosticosCorretoras().size());
        var zero = p.diagnosticosCorretoras().get(1);
        assertEquals("Z", zero.nome()); assertEquals(0, zero.quantidadeFinal().signum());
        assertTrue(zero.possuiDeficitHistorico());
        assertEquals(2, zero.primeiroDeficit().posicaoCronologica().get("ordemNoDia"));
        assertTrue(p.diagnosticosCorretoras().get(2).saldoNegativoAtual());
        assertEquals(4, stats.getPrepareStatementCount()); // fontes completas no mesmo snapshot, sem consultas por grupo
        assertEquals(0, stats.getEntityInsertCount());
        assertEquals(0, stats.getEntityUpdateCount());
        assertEquals(0, stats.getEntityDeleteCount());
    }

    private Carteira carteira() { return new Carteira("Teste", OffsetDateTime.now()); }
    private Corretora broker(String cnpj, String nome) {
        return new Corretora(cnpj, nome, nome, null, null, "00000000", "Rua", null,
                null, "Bairro", "Cidade", "SP", "ATIVA", OffsetDateTime.now());
    }
    private void op(Carteira c, Acao a, Corretora b, TipoOperacao tipo, String q, int ordem) {
        BigDecimal quantidade = new BigDecimal(q);
        em.persist(new Operacao(c, a, b, tipo, quantidade, BigDecimal.TEN,
                LocalDate.of(2026, 10, 11), ordem, quantidade.multiply(BigDecimal.TEN)));
    }
}
