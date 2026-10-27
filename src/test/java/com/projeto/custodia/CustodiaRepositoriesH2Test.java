package com.projeto.custodia;

import com.projeto.custodia.support.CustodiaH2Test;
import com.projeto.entities.*;
import com.projeto.repositories.EstornoMovimentacaoCustodiaRepository;
import com.projeto.repositories.MovimentacaoCustodiaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import liquibase.integration.spring.SpringLiquibase;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Somente consultas existentes: sem ordenação presumida, serviços ou inicialização manual de proxies. */
@CustodiaH2Test
@Transactional
class CustodiaRepositoriesH2Test {
    private static final OffsetDateTime REGISTRO = OffsetDateTime.parse("2026-10-14T12:00:00.123456-03:00");
    private static final Clock CLOCK = Clock.fixed(REGISTRO.toInstant(), REGISTRO.getOffset());
    private static final LocalDate DIA = LocalDate.of(2001, 2, 3);

    @PersistenceContext private EntityManager em;
    @Autowired private EntityManagerFactory factory;
    @Autowired private MovimentacaoCustodiaRepository movimentos;
    @Autowired private EstornoMovimentacaoCustodiaRepository estornos;
    @Autowired private DataSource dataSource;
    @Autowired private SpringLiquibase liquibase;
    @Autowired private ConfigurableApplicationContext context;
    private Statistics statistics;
    private boolean estatisticasAnteriores;
    private long sqlFixtures;

    @BeforeEach
    void verificaIsolamentoEIniciaMedicao() throws Exception {
        assertEquals("2.5.250", org.h2.engine.Constants.class.getField("VERSION").get(null));
        assertEquals(1, context.getBeansOfType(DataSource.class).size());
        assertSame(dataSource, liquibase.getDataSource());
        assertSame(dataSource, ((EntityManagerFactoryInfo) factory).getDataSource());
        assertArrayEquals(new String[]{"custodia-h2"}, context.getEnvironment().getActiveProfiles());
        assertEquals("org.h2.Driver", ReflectionTestUtils.getField(dataSource, "driver").getClass().getName());
        String url = (String) ReflectionTestUtils.getField(dataSource, "url");
        String prefix = "jdbc:h2:mem:custodia_";
        assertNotNull(url);
        assertTrue(url.startsWith(prefix));
        assertEquals(prefix + UUID.fromString(url.substring(prefix.length())), url);
        try (var connection = dataSource.getConnection()) {
            assertEquals("H2", connection.getMetaData().getDatabaseProductName());
            assertEquals(url, connection.getMetaData().getURL());
        }
        statistics = factory.unwrap(SessionFactory.class).getStatistics();
        estatisticasAnteriores = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @AfterEach
    void restauraEstatisticas() {
        if (statistics != null) statistics.setStatisticsEnabled(estatisticasAnteriores);
    }

    @ParameterizedTest
    @ValueSource(strings = {"carteira", "acao", "detalhe"})
    void movimentacoesFiltramCarteiraAcaoEIdComAssociacoesCompletas(String consulta) {
        Fixture f = fixture(6);
        List<MovimentacaoCustodia> resultado = medir("movimentacoes/" + consulta,
                () -> consultarMovimentos(consulta, f), this::lerMovimentos);
        Set<Long> esperados = switch (consulta) {
            case "carteira" -> f.movimentos().keySet();
            case "acao" -> f.acaoA();
            default -> Set.of(f.atribuicao());
        };
        confereIds(esperados, resultado.stream().map(MovimentacaoCustodia::getId).toList());
        for (var m : resultado) {
            assertEquals(f.carteira(), m.getCarteiraId());
            assertFalse(f.outraCarteiraMovimentos().contains(m.getId()));
            Esperado esperado = f.movimentos().get(m.getId());
            assertEquals(esperado.acao(), m.getAcaoId());
            assertEquals(esperado.origem(), m.getCorretoraOrigemId());
            assertEquals(esperado.destino(), m.getCorretoraDestinoId());
            assertEquals(esperado.natureza(), m.getNatureza());
        }
        // Os repositórios não contratam ordem: não ordenar a lista para simular ordem de auditoria/replay.
    }

    @ParameterizedTest
    @ValueSource(strings = {"carteira-vazia", "carteira-inexistente", "acao-inexistente", "acao-de-outra-carteira"})
    void movimentacoesSemCorrespondenciaRetornamVazio(String caso) {
        Fixture f = fixture(6);
        List<MovimentacaoCustodia> resultado = medir("movimentacoes/vazio/" + caso, () -> switch (caso) {
            case "carteira-vazia" -> movimentos.listarPorCarteira(f.vazia());
            case "carteira-inexistente" -> movimentos.listarPorCarteira(Long.MAX_VALUE);
            case "acao-inexistente" -> movimentos.listarPorCarteiraEAcao(f.carteira(), Long.MAX_VALUE);
            default -> movimentos.listarPorCarteiraEAcao(f.carteira(), f.acaoExclusivaOutra());
        }, this::lerMovimentos);
        assertTrue(resultado.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"inexistente", "outra-carteira"})
    void detalheNaoExpoeMovimentacaoDeOutraCarteira(String caso) {
        Fixture f = fixture(6);
        var resultado = medir("movimentacoes/detalhe/" + caso,
                () -> movimentos.buscarPorCarteiraEId(f.carteira(), caso.equals("inexistente")
                        ? Long.MAX_VALUE : f.outraCarteiraMovimentos().get(0)),
                optional -> optional.ifPresent(m -> lerMovimentos(List.of(m))));
        assertTrue(resultado.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"carteira", "acao", "lote"})
    void estornosFiltramCarteiraAcaoEOriginaisEmLote(String consulta) {
        Fixture f = fixture(6);
        var resultado = medir("estornos/" + consulta, () -> consultarEstornos(consulta, f), this::lerEstornos);
        Set<Long> originais = consulta.equals("carteira") ? f.estornos().keySet()
                : consulta.equals("acao") ? f.acaoA() : Set.of(f.transferencia(), f.atribuicao());
        confereIds(originais.stream().map(f.estornos()::get).collect(Collectors.toSet()),
                resultado.stream().map(EstornoMovimentacaoCustodia::getId).toList());
        confereIds(originais, resultado.stream().map(EstornoMovimentacaoCustodia::getMovimentacaoOriginalId).toList());
        for (var e : resultado) {
            assertEquals(f.carteira(), e.getMovimentacaoOriginal().getCarteiraId());
            assertFalse(f.outraCarteiraMovimentos().contains(e.getMovimentacaoOriginalId()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"carteira-vazia", "carteira-inexistente", "acao-sem-estorno", "acao-inexistente",
            "lote-sem-estorno", "lote-outra-carteira", "lote-inexistente"})
    void estornosSemCorrespondenciaRetornamVazio(String caso) {
        Fixture f = fixture(6);
        var resultado = medir("estornos/vazio/" + caso, () -> switch (caso) {
            case "carteira-vazia" -> estornos.listarPorCarteira(f.vazia());
            case "carteira-inexistente" -> estornos.listarPorCarteira(Long.MAX_VALUE);
            case "acao-sem-estorno" -> estornos.listarPorCarteiraEAcao(f.carteira(), f.acaoSemEstorno());
            case "acao-inexistente" -> estornos.listarPorCarteiraEAcao(f.carteira(), Long.MAX_VALUE);
            case "lote-sem-estorno" -> estornos.listarPorCarteiraEOriginais(f.carteira(), List.of(f.semEstorno()));
            case "lote-outra-carteira" -> estornos.listarPorCarteiraEOriginais(f.carteira(), f.outraCarteiraMovimentos());
            default -> estornos.listarPorCarteiraEOriginais(f.carteira(), List.of(Long.MAX_VALUE));
        }, this::lerEstornos);
        assertTrue(resultado.isEmpty());
        // Lista de IDs vazia é responsabilidade do chamador, conforme Javadoc; não invocar IN vazio.
    }

    @ParameterizedTest
    @ValueSource(ints = {6, 18})
    void quantidadeDeSqlNaoCresceComNumeroDeRegistros(int quantidade) {
        Fixture f = fixture(quantidade);
        for (String consulta : List.of("carteira", "acao", "detalhe")) {
            var resultado = medir("N+1/movimentacoes/" + consulta + "/n=" + quantidade,
                    () -> consultarMovimentos(consulta, f), this::lerMovimentos);
            int esperado = consulta.equals("carteira") ? quantidade + 1 : consulta.equals("acao") ? quantidade / 2 : 1;
            assertEquals(esperado, resultado.size());
        }
        for (String consulta : List.of("carteira", "acao", "lote")) {
            var resultado = medir("N+1/estornos/" + consulta + "/n=" + quantidade,
                    () -> consultarEstornos(consulta, f), this::lerEstornos);
            assertEquals(consulta.equals("carteira") ? quantidade : consulta.equals("acao") ? quantidade / 2 : 2,
                    resultado.size());
        }
    }

    private List<MovimentacaoCustodia> consultarMovimentos(String consulta, Fixture f) {
        return switch (consulta) {
            case "carteira" -> movimentos.listarPorCarteira(f.carteira());
            case "acao" -> movimentos.listarPorCarteiraEAcao(f.carteira(), f.acao());
            default -> movimentos.buscarPorCarteiraEId(f.carteira(), f.atribuicao()).stream().toList();
        };
    }

    private List<EstornoMovimentacaoCustodia> consultarEstornos(String consulta, Fixture f) {
        return switch (consulta) {
            case "carteira" -> estornos.listarPorCarteira(f.carteira());
            case "acao" -> estornos.listarPorCarteiraEAcao(f.carteira(), f.acao());
            // Misturar IDs de carteiras diferentes, ID inexistente e original sem estorno.
            default -> estornos.listarPorCarteiraEOriginais(f.carteira(), List.of(f.transferencia(), f.atribuicao(),
                    f.outraCarteiraMovimentos().get(0), f.semEstorno(), Long.MAX_VALUE));
        };
    }

    private <T> T medir(String nome, Supplier<T> consulta, Consumer<T> leitura) {
        em.flush();
        em.clear();
        statistics.clear();
        T resultado = consulta.get();
        long principal = statistics.getPrepareStatementCount();
        leitura.accept(resultado);
        long adicionais = statistics.getPrepareStatementCount() - principal;
        System.out.printf("SQL %s fixtures=%d principal=%d associacoes=%d%n", nome, sqlFixtures, principal, adicionais);
        assertEquals(1, principal, nome + ": consulta principal incluindo eventuais cargas secundárias");
        assertEquals(0, adicionais, nome + ": SQL adicional ao acessar associações");
        assertEquals(0, statistics.getEntityInsertCount());
        assertEquals(0, statistics.getEntityUpdateCount());
        assertEquals(0, statistics.getEntityDeleteCount());
        return resultado;
    }

    private void lerMovimentos(List<MovimentacaoCustodia> lista) {
        var util = factory.getPersistenceUnitUtil();
        for (var m : lista) {
            for (String campo : List.of("carteira", "acao", "corretoraDestino", "corretoraOrigem")) {
                assertTrue(util.isLoaded(m, campo), campo + " deve vir carregado pela consulta");
            }
            assertNotNull(m.getCarteira().getNome());
            assertNotNull(m.getAcao().getTicker());
            assertNotNull(m.getAcao().getMercado());
            assertNotNull(m.getCorretoraDestino().getNomeFantasia());
            if (m.getNatureza() == NaturezaMovimentacaoCustodia.ATRIBUICAO) {
                assertNull(m.getCorretoraOrigem());
            } else {
                assertNotNull(m.getCorretoraOrigem().getNomeFantasia());
                assertNotEquals(m.getCorretoraOrigemId(), m.getCorretoraDestinoId());
            }
            assertEquals(DIA, m.getDataEfetiva());
            assertEquals(new BigDecimal("10.000000"), m.getQuantidade());
            assertDoesNotThrow(m::validarEstruturaHistorica);
        }
    }

    private void lerEstornos(List<EstornoMovimentacaoCustodia> lista) {
        for (var e : lista) {
            assertTrue(factory.getPersistenceUnitUtil().isLoaded(e, "movimentacaoOriginal"));
            lerMovimentos(List.of(e.getMovimentacaoOriginal()));
            assertEquals("Motivo sintético", e.getMotivo());
            assertEquals(DIA, e.getDataEfetiva());
            assertEquals(e.getMovimentacaoOriginal().getPosicaoRacional(), e.getPosicaoRacional());
            assertDoesNotThrow(e::validarEstruturaHistorica);
        }
    }

    private void confereIds(Set<Long> esperado, List<Long> encontrados) {
        assertEquals(esperado.size(), encontrados.size(), "Sem truncamento ou duplicatas");
        assertEquals(esperado, new HashSet<>(encontrados));
    }

    private Fixture fixture(int quantidade) {
        var carteira = new Carteira("Selecionada", REGISTRO);
        var outra = new Carteira("Outra", REGISTRO);
        var vazia = new Carteira("Vazia", REGISTRO);
        em.persist(carteira); em.persist(outra); em.persist(vazia);
        var acaoA = acao("SINTA");
        var acaoB = acao("SINTB");
        var acaoC = acao("SINTC");
        var acaoExclusiva = acao("SINTOUTRA");
        Map<Long, Esperado> esperados = new java.util.HashMap<>();
        Map<Long, Long> vinculos = new java.util.HashMap<>();
        Set<Long> idsAcaoA = new HashSet<>();
        long transferencia = 0, atribuicao = 0;
        for (int i = 0; i < quantidade; i++) {
            var acao = i % 2 == 0 ? acaoA : acaoB;
            var origem = corretora(i * 2 + 1);
            var destino = corretora(i * 2 + 2);
            var m = movimento(carteira, acao, i % 2 == 0 ? origem : null, destino, i);
            esperados.put(m.getId(), new Esperado(acao.getId(), m.getCorretoraOrigemId(), destino.getId(), m.getNatureza()));
            var e = new EstornoMovimentacaoCustodia(null, m, "Motivo sintético", CLOCK);
            em.persist(e);
            vinculos.put(m.getId(), e.getId());
            if (i % 2 == 0) idsAcaoA.add(m.getId());
            if (i == 0) transferencia = m.getId();
            if (i == 1) atribuicao = m.getId();
        }
        var destinoExtra = corretora(900);
        var ativa = movimento(carteira, acaoC, null, destinoExtra, quantidade);
        esperados.put(ativa.getId(), new Esperado(acaoC.getId(), null, destinoExtra.getId(), ativa.getNatureza()));
        List<Long> outras = new ArrayList<>();
        for (Acao acao : List.of(acaoA, acaoExclusiva)) {
            var m = movimento(outra, acao, null, destinoExtra, quantidade + 1);
            outras.add(m.getId());
            em.persist(new EstornoMovimentacaoCustodia(null, m, "Motivo sintético", CLOCK));
        }
        em.flush();
        sqlFixtures = statistics.getPrepareStatementCount();
        assertTrue(sqlFixtures > 0);
        em.clear();
        return new Fixture(carteira.getId(), vazia.getId(), acaoA.getId(), acaoC.getId(), acaoExclusiva.getId(),
                transferencia, atribuicao, ativa.getId(), esperados, vinculos, idsAcaoA, outras);
    }

    private Acao acao(String ticker) {
        var a = new Acao(ticker, "Empresa sintética", Mercado.BRASIL, Moeda.BRL, BigDecimal.TEN, REGISTRO);
        em.persist(a);
        return a;
    }

    private Corretora corretora(int numero) {
        var c = new Corretora(String.format(java.util.Locale.ROOT, "%014d", numero), "Corretora " + numero,
                "Nome " + numero, null, null, "00000000", "Rua sintética", "1", null, "Bairro", "Cidade", "SP", "ATIVA", REGISTRO);
        em.persist(c);
        return c;
    }

    private MovimentacaoCustodia movimento(Carteira carteira, Acao acao, Corretora origem, Corretora destino, int ordem) {
        var m = new MovimentacaoCustodia(null, carteira, acao, origem == null
                ? NaturezaMovimentacaoCustodia.ATRIBUICAO : NaturezaMovimentacaoCustodia.TRANSFERENCIA,
                origem, destino, BigDecimal.TEN, DIA, 0,
                PosicaoRacionalCustodia.deTexto(Integer.toString(ordem + 1), Integer.toString(ordem + 2)), CLOCK);
        em.persist(m);
        return m;
    }

    private record Esperado(Long acao, Long origem, Long destino, NaturezaMovimentacaoCustodia natureza) { }
    private record Fixture(Long carteira, Long vazia, Long acao, Long acaoSemEstorno, Long acaoExclusivaOutra,
            long transferencia, long atribuicao, long semEstorno, Map<Long, Esperado> movimentos,
            Map<Long, Long> estornos, Set<Long> acaoA, List<Long> outraCarteiraMovimentos) { }
}
