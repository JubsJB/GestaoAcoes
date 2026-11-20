package com.projeto.custodia;

import com.projeto.custodia.support.CustodiaH2Test;
import com.projeto.entities.*;
import com.projeto.repositories.*;
import com.projeto.services.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import static com.projeto.services.EventoQuantitativo.*;
import static org.junit.jupiter.api.Assertions.*;

/** Ponte de leitura real, sem serviços de produção, candidatos, upgrade ou rollback Liquibase. */
@CustodiaH2Test
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustodiaPontePersistenciaH2Test {
    private static final LocalDate DIA = LocalDate.of(2001, 2, 3);
    private static final OffsetDateTime REGISTRO = OffsetDateTime.parse("2026-10-14T12:00:00.123456Z");
    private static final Clock CLOCK = Clock.fixed(REGISTRO.toInstant(), REGISTRO.getOffset());
    @PersistenceContext private EntityManager em;
    @Autowired private EntityManagerFactory factory;
    @Autowired private OperacaoRepository operacoes;
    @Autowired private MovimentacaoCustodiaRepository movimentos;
    @Autowired private EstornoMovimentacaoCustodiaRepository estornos;
    @Autowired private DataSource dataSource;
    @Autowired private SpringLiquibase liquibase;
    @Autowired private ConfigurableApplicationContext context;
    private Statistics stats;
    private boolean estatisticasAnteriores;

    @BeforeEach void verificaProtecoesAntesDasFixtures() throws Exception {
        assertEquals("2.5.250", org.h2.engine.Constants.class.getField("VERSION").get(null));
        assertEquals(1, context.getBeansOfType(DataSource.class).size());
        assertSame(dataSource, liquibase.getDataSource());
        assertSame(dataSource, ((EntityManagerFactoryInfo) factory).getDataSource());
        assertEquals("validate", factory.getProperties().get("hibernate.hbm2ddl.auto"));
        assertArrayEquals(new String[]{"custodia-h2"}, context.getEnvironment().getActiveProfiles());
        assertEquals("org.h2.Driver", ReflectionTestUtils.getField(dataSource, "driver").getClass().getName());
        String url = (String) ReflectionTestUtils.getField(dataSource, "url");
        String prefix = "jdbc:h2:mem:custodia_";
        assertNotNull(url); assertTrue(url.startsWith(prefix));
        assertEquals(prefix + UUID.fromString(url.substring(prefix.length())), url);
        try (var conexao = dataSource.getConnection()) {
            assertEquals("H2", conexao.getMetaData().getDatabaseProductName());
            assertEquals(url, conexao.getMetaData().getURL());
        }
        stats = factory.unwrap(SessionFactory.class).getStatistics();
        estatisticasAnteriores = stats.isStatisticsEnabled(); stats.setStatisticsEnabled(true); stats.clear();
    }

    @AfterEach void restauraEstatisticas() { if (stats != null) stats.setStatisticsEnabled(estatisticasAnteriores); }

    @Test @Order(1) void idsReaisIguaisGeradosEmTabelasDiferentesConservamDominios() {
        // Primeiro cenário no banco exclusivo da classe: identities de ambas as tabelas começam em 1.
        var f = fixture(Mercado.BRASIL, 1);
        var r = carregar(f);
        Set<Long> idsFinanceiros = r.eventos().stream().filter(e -> e.identidade().dominio() == Dominio.FINANCEIRO)
                .map(e -> e.identidade().idPersistido()).collect(Collectors.toSet());
        var compartilhadas = r.eventos().stream().filter(e -> e.identidade().dominio() == Dominio.CUSTODIA
                && idsFinanceiros.contains(e.identidade().idPersistido())).toList();
        assertFalse(compartilhadas.isEmpty());
        for (var custodia : compartilhadas) {
            var financeiro = r.eventos().stream().filter(e -> e.identidade().equals(
                    Identidade.persistida(Dominio.FINANCEIRO, custodia.identidade().idPersistido()))).findFirst().orElseThrow();
            assertNotEquals(financeiro.identidade(), custodia.identidade());
        }
        confereReplay(f, r);
    }

    @ParameterizedTest @CsvSource({"BRASIL,1", "BRASIL,5", "BRASIL,20", "EUA,1", "EUA,5", "EUA,20"})
    void ponteCompletaMantemTresConsultasComVolumesCrescentes(Mercado mercado, int repeticoes) {
        var f = fixture(mercado, repeticoes); var r = carregar(f);
        assertEquals(3L, r.sqlConsultas());
        assertEquals(3 * repeticoes, r.financeiros().size());
        assertEquals(3 * repeticoes, r.custodias().size());
        assertEquals(repeticoes, r.estornos().size());
        assertEquals(6 * repeticoes, r.eventos().size());
        assertEquals(repeticoes, r.eventos().stream().filter(EventoQuantitativo::neutralizado).count());
        assertEquals(Set.copyOf(f.financeiros()), r.financeiros().stream().map(Operacao::getId).collect(Collectors.toSet()));
        assertEquals(Set.copyOf(f.custodias()), r.custodias().stream().map(MovimentacaoCustodia::getId).collect(Collectors.toSet()));
        assertEquals(Set.copyOf(f.estornos()), r.estornos().stream().map(EstornoMovimentacaoCustodia::getId).collect(Collectors.toSet()));
        for (int i = 0; i < repeticoes; i++) {
            var data = DIA.plusDays(i * 3L);
            var dia = r.eventos().stream().filter(e -> e.posicao().dataEfetiva().equals(data)).toList();
            assertEquals(List.of(Natureza.COMPRA, Natureza.TRANSFERENCIA, Natureza.COMPRA,
                    Natureza.TRANSFERENCIA, Natureza.ATRIBUICAO), dia.stream().map(EventoQuantitativo::natureza).toList());
            assertEquals(List.of(1, 1, 2, 2, 2), dia.stream().map(e -> e.posicao().ancoraFinanceira()).toList());
            assertEquals("2/3", dia.get(1).posicao().fracao().toString());
            assertEquals("1/2", dia.get(3).posicao().fracao().toString()); assertTrue(dia.get(3).neutralizado());
            assertEquals("10/11", dia.get(4).posicao().fracao().toString());
            assertTrue(r.eventos().stream().anyMatch(e -> e.posicao().dataEfetiva().equals(data.plusDays(1)) && e.natureza() == Natureza.VENDA));
        }
        confereReplay(f, r);
        assertTrue(r.replay().estadoFinal().primeirosNegativos().isEmpty());
    }

    @Test void deficitLegadoEHorizontePosteriorSaoReproduzidosSemSaneamento() {
        var f = fixture(Mercado.BRASIL, 0);
        var carteira = em.find(Carteira.class, f.carteira()); var acao = em.find(Acao.class, f.acao());
        var a = em.find(Corretora.class, f.a()); var b = em.find(Corretora.class, f.b());
        var compra = op(carteira, acao, a, TipoOperacao.COMPRA, new BigDecimal("10"), DIA, 1);
        var venda = op(carteira, acao, b, TipoOperacao.VENDA, new BigDecimal("4"), DIA, 2);
        op(carteira, acao, b, TipoOperacao.COMPRA, new BigDecimal("1"), DIA.plusYears(10), 1);
        var r = carregar(f); assertEquals(3, r.financeiros().size()); assertEquals(2, r.sqlConsultas());
        var estado = r.replay().estadoFinal(); decimal("7", estado.quantidadeConsolidada());
        decimal("10", estado.saldo(Grupo.corretora(a.getId()))); decimal("-3", estado.saldo(Grupo.corretora(b.getId())));
        assertEquals(Identidade.persistida(Dominio.FINANCEIRO, venda.getId()), estado.primeirosNegativos().get(Grupo.corretora(b.getId())).identidade());
        assertEquals(compra.getId(), r.eventos().get(0).identidade().idPersistido()); assertTrue(estado.reconciliada());
    }

    @Test void contextoSemHistoricoNaoRecebeEventosDeOutrasCarteirasOuAcoes() {
        var f = fixture(Mercado.EUA, 0); var r = carregar(f);
        assertTrue(r.eventos().isEmpty()); assertTrue(r.estornos().isEmpty());
        assertEquals(2, r.sqlConsultas()); assertEquals(0, r.replay().estadoFinal().quantidadeConsolidada().signum());
    }

    @Test void contextoPermiteSomenteOsTresRepositoriosSemServicesOuHttp() {
        assertEquals(Set.of("operacaoRepository", "movimentacaoCustodiaRepository", "estornoMovimentacaoCustodiaRepository"),
                context.getBeansOfType(org.springframework.data.repository.Repository.class).keySet());
        assertFalse(((org.springframework.beans.factory.support.DefaultListableBeanFactory) context.getBeanFactory()).isAllowBeanDefinitionOverriding());
        for (String nome : context.getBeanDefinitionNames()) {
            Class<?> tipo = context.getType(nome);
            if (tipo != null) {
                assertFalse(tipo.getName().startsWith("com.projeto.services."), tipo.getName());
                assertFalse(tipo.getName().startsWith("com.projeto.integrations."), tipo.getName());
                assertFalse(tipo.getName().contains("ServletWebServer"), tipo.getName());
            }
        }
    }

    /** Clear entre as consultas impede uma carga anterior de esconder um fetch ausente. */
    private Leitura carregar(Fixture f) {
        em.flush(); long sqlFixtures = stats.getPrepareStatementCount(); em.clear(); stats.clear();
        var fs = consultar(() -> operacoes.listarParaReplayQuantitativo(f.carteira(), f.acao()), this::lerFinanceiros);
        em.clear();
        var cs = consultar(() -> movimentos.listarPorCarteiraEAcao(f.carteira(), f.acao()), this::lerCustodias);
        em.clear();
        var es = cs.isEmpty() ? List.<EstornoMovimentacaoCustodia>of() : consultar(
                () -> estornos.listarPorCarteiraEOriginais(f.carteira(), cs.stream().map(MovimentacaoCustodia::getId).toList()),
                lista -> lista.forEach(e -> {
                    assertTrue(Hibernate.isInitialized(e.getMovimentacaoOriginal()));
                    lerCustodias(List.of(e.getMovimentacaoOriginal())); e.validarEstruturaHistorica();
                }));
        em.clear(); // Também comprova adaptação de entidades destacadas com associações completas.
        long sqlConsultas = stats.getPrepareStatementCount();
        Map<Long, EstornoMovimentacaoCustodia> porOriginal = es.stream().collect(Collectors.toMap(EstornoMovimentacaoCustodia::getMovimentacaoOriginalId, e -> e));
        var vinculos = cs.stream().map(c -> porOriginal.containsKey(c.getId())
                ? VinculoEstornoConsultado.vinculoPresente(c.getId(), porOriginal.get(c.getId()))
                : VinculoEstornoConsultado.ausenciaVerificada(c.getId())).toList();
        var adaptador = new AdaptadorEventosQuantitativos(); var normalizador = new NormalizadorEventosQuantitativos();
        var fontes = adaptador.adaptar(f.carteira(), f.acao(), fs, cs, vinculos);
        var eventos = normalizador.normalizar(f.carteira(), f.acao(), fontes.financeiros(), fontes.custodias());
        var replay = new MotorReplayQuantitativo().reproduzir(eventos);
        var reversas = new ArrayList<>(cs); Collections.reverse(reversas);
        var financeirosReversos = new ArrayList<>(fs); Collections.reverse(financeirosReversos);
        var outrasFontes = adaptador.adaptar(f.carteira(), f.acao(), financeirosReversos, reversas, vinculos);
        assertEquals(eventos, normalizador.normalizar(f.carteira(), f.acao(), outrasFontes.financeiros(), outrasFontes.custodias()));
        assertEquals(sqlConsultas, stats.getPrepareStatementCount(), "Adaptação/normalização/replay não executam SQL");
        System.out.printf("PONTE mercado=%s rodadas=%d sqlFixtures=%d sqlConsultas=%d sqlAssociacoes=0 sqlAdaptacaoReplay=0%n",
                f.mercado(), f.repeticoes(), sqlFixtures, sqlConsultas);
        return new Leitura(fs, cs, es, eventos, replay, sqlConsultas);
    }

    private <T> List<T> consultar(Supplier<List<T>> consulta, Consumer<List<T>> ler) {
        long antes = stats.getPrepareStatementCount(); var resultado = consulta.get();
        assertEquals(antes + 1, stats.getPrepareStatementCount(), "Uma instrução por consulta principal");
        ler.accept(resultado);
        assertEquals(antes + 1, stats.getPrepareStatementCount(), "Associações não podem gerar SQL adicional");
        return resultado;
    }

    private void lerFinanceiros(List<Operacao> lista) {
        for (var o : lista) {
            assertTrue(Hibernate.isInitialized(o.getCarteira())); assertTrue(Hibernate.isInitialized(o.getAcao()));
            assertTrue(Hibernate.isInitialized(o.getCorretora()));
            o.getCarteira().getNome(); o.getAcao().getMercado();
            if (o.getCorretora() != null) o.getCorretora().getRazaoSocial();
        }
    }
    private void lerCustodias(List<MovimentacaoCustodia> lista) {
        for (var m : lista) {
            assertTrue(Hibernate.isInitialized(m.getCarteira())); assertTrue(Hibernate.isInitialized(m.getAcao()));
            assertTrue(Hibernate.isInitialized(m.getCorretoraOrigem())); assertTrue(Hibernate.isInitialized(m.getCorretoraDestino()));
            m.getCarteira().getNome(); m.getAcao().getMercado(); m.getCorretoraDestino().getRazaoSocial();
            if (m.getCorretoraOrigem() != null) m.getCorretoraOrigem().getRazaoSocial();
            m.validarEstruturaHistorica();
        }
    }

    private void confereReplay(Fixture f, Leitura r) {
        var fator = f.unidade().multiply(BigDecimal.valueOf(f.repeticoes())); var estado = r.replay().estadoFinal();
        assertEquals(0, fator.multiply(new BigDecimal("14")).compareTo(estado.quantidadeConsolidada()));
        assertEquals(0, fator.multiply(new BigDecimal("7")).compareTo(estado.saldo(Grupo.corretora(f.a()))));
        assertEquals(0, fator.multiply(new BigDecimal("4")).compareTo(estado.saldo(Grupo.corretora(f.b()))));
        assertEquals(0, fator.multiply(new BigDecimal("3")).compareTo(estado.saldo(Grupo.semCorretora())));
        assertEquals(r.eventos().size(), r.replay().checkpoints().size()); assertTrue(estado.reconciliada());
        for (var e : r.eventos()) { assertEquals(f.carteira(), e.carteiraId()); assertEquals(f.acao(), e.acaoId()); }
    }

    private Fixture fixture(Mercado mercado, int repeticoes) {
        var carteira = new Carteira("Carteira sintética", REGISTRO); var outraCarteira = new Carteira("Outra sintética", REGISTRO);
        var acao = acao("SINTA", mercado); var outraAcao = acao("SINTB", mercado);
        var a = corretora("11111111000111"); var b = corretora("22222222000122");
        for (Object e : List.of(carteira, outraCarteira, acao, outraAcao, a, b)) em.persist(e);
        var unidade = mercado == Mercado.BRASIL ? BigDecimal.ONE : new BigDecimal("0.000001");
        List<Long> fs = new ArrayList<>(); List<Long> cs = new ArrayList<>(); List<Long> es = new ArrayList<>();
        for (int i = 0; i < repeticoes; i++) {
            var dia = DIA.plusDays(i * 3L);
            fs.add(op(carteira, acao, a, TipoOperacao.COMPRA, unidade.multiply(BigDecimal.TEN), dia, 1).getId());
            fs.add(op(carteira, acao, null, TipoOperacao.COMPRA, unidade.multiply(new BigDecimal("5")), dia, 2).getId());
            fs.add(op(carteira, acao, b, TipoOperacao.VENDA, unidade, dia.plusDays(1), 1).getId());
            cs.add(mov(carteira, acao, a, b, unidade.multiply(new BigDecimal("3")), dia, 1, "2", "3").getId());
            cs.add(mov(carteira, acao, null, b, unidade.multiply(new BigDecimal("2")), dia, 2, "10", "11").getId());
            var original = mov(carteira, acao, b, a, unidade, dia, 2, "1", "2"); cs.add(original.getId());
            var estorno = new EstornoMovimentacaoCustodia(null, original, "Sintético", CLOCK); em.persist(estorno); es.add(estorno.getId());
        }
        // Sentinelas persistidas fora do contexto: nenhuma das três consultas deve incluí-las.
        for (var par : List.of(new Object[]{outraCarteira, acao}, new Object[]{carteira, outraAcao})) {
            var c = (Carteira) par[0]; var at = (Acao) par[1];
            op(c, at, null, TipoOperacao.COMPRA, unidade.multiply(BigDecimal.TEN), DIA, 1);
            var original = mov(c, at, null, b, unidade, DIA, 0, "1", "2");
            em.persist(new EstornoMovimentacaoCustodia(null, original, null, CLOCK));
        }
        return new Fixture(carteira.getId(), acao.getId(), a.getId(), b.getId(), mercado, unidade, repeticoes, fs, cs, es);
    }

    private Operacao op(Carteira c, Acao acao, Corretora corretora, TipoOperacao tipo, BigDecimal q, LocalDate dia, int ordem) {
        var o = new Operacao(c, acao, corretora, tipo, q, BigDecimal.TEN, dia, ordem, q.multiply(BigDecimal.TEN)); em.persist(o); return o;
    }
    private MovimentacaoCustodia mov(Carteira c, Acao acao, Corretora origem, Corretora destino, BigDecimal q,
            LocalDate dia, int ancora, String n, String d) {
        var m = new MovimentacaoCustodia(null, c, acao, origem == null ? NaturezaMovimentacaoCustodia.ATRIBUICAO
                : NaturezaMovimentacaoCustodia.TRANSFERENCIA, origem, destino, q, dia, ancora, PosicaoRacionalCustodia.deTexto(n, d), CLOCK);
        em.persist(m); return m;
    }
    private Acao acao(String ticker, Mercado mercado) {
        return new Acao(ticker, "Empresa sintética", mercado, mercado == Mercado.BRASIL ? Moeda.BRL : Moeda.USD, BigDecimal.TEN, REGISTRO);
    }
    private Corretora corretora(String cnpj) {
        return new Corretora(cnpj, "Corretora sintética", null, null, null, "00000000", "Rua", "1", null, "Bairro", "Cidade", "SP", "ATIVA", REGISTRO);
    }
    private static void decimal(String esperado, BigDecimal atual) { assertEquals(0, new BigDecimal(esperado).compareTo(atual)); }
    private record Fixture(long carteira, long acao, long a, long b, Mercado mercado, BigDecimal unidade,
            int repeticoes, List<Long> financeiros, List<Long> custodias, List<Long> estornos) { }
    private record Leitura(List<Operacao> financeiros, List<MovimentacaoCustodia> custodias,
            List<EstornoMovimentacaoCustodia> estornos, List<EventoQuantitativo> eventos, MotorReplayQuantitativo.Resultado replay, long sqlConsultas) { }
}
