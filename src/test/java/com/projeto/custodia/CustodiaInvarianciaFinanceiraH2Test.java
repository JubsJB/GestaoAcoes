package com.projeto.custodia;

import com.projeto.custodia.support.CustodiaH2IsolationInitializer;
import com.projeto.custodia.support.CustodiaH2TestConfiguration;
import com.projeto.entities.*;
import com.projeto.mappers.*;
import com.projeto.repositories.*;
import com.projeto.services.*;
import com.projeto.services.exceptions.ConstraintNameExtractor;
import com.projeto.validation.TickerNormalizer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/** Regressão através dos serviços reais; fixtures confirmadas e leituras após cada commit. */
@ExtendWith(SpringExtension.class)
@ActiveProfiles(value = "custodia-h2", inheritProfiles = false)
@ContextConfiguration(classes = CustodiaInvarianciaFinanceiraH2Test.Config.class,
        initializers = CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustodiaInvarianciaFinanceiraH2Test {
    @TestConfiguration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackages = "com.projeto.repositories",
            excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = Referencias.class))
    @Import({CustodiaH2TestConfiguration.class, TransferenciaCustodiaService.class,
            AtribuicaoCustodiaService.class, EstornoCustodiaService.class,
            TransferenciaCustodiaPersistenceService.class, TickerNormalizer.class,
            PosicaoService.class, ResumoCarteiraService.class, SnapshotCarteiraService.class,
            CalculadoraPosicao.class, CalculadoraRentabilidade.class, AgregadorPosicoesPorMoeda.class,
            PosicaoMapper.class, ResumoCarteiraMapper.class, SnapshotCarteiraMapper.class,
            ConstraintNameExtractor.class})
    static class Config { }
    public static class Referencias implements TypeFilter {
        @Override public boolean match(MetadataReader r, MetadataReaderFactory f) {
            return !Set.of(CarteiraRepository.class.getName(), AcaoRepository.class.getName(),
                    CorretoraRepository.class.getName(), SnapshotCarteiraRepository.class.getName(),
                    SnapshotCarteiraMoedaRepository.class.getName()).contains(r.getClassMetadata().getClassName());
        }
    }
    @Autowired TransferenciaCustodiaService transferencias;
    @Autowired AtribuicaoCustodiaService atribuicoes;
    @Autowired EstornoCustodiaService estornos;
    @Autowired PosicaoService posicoes;
    @Autowired ResumoCarteiraService resumos;
    @Autowired SnapshotCarteiraService snapshots;
    @Autowired OperacaoRepository operacoes;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean Clock clock;
    @PersistenceContext EntityManager em;
    static final LocalDate DIA = LocalDate.of(2026, 10, 1);
    static final OffsetDateTime REGISTRO = OffsetDateTime.parse("2026-10-24T12:00:00Z");
    <T> T tx(Function<EntityManager, T> trabalho) {
        return new TransactionTemplate(manager).execute(status -> trabalho.apply(em));
    }
    record Fixture(Long carteira, Long acao, Long origem, Long destino, String ticker) { }

    @ParameterizedTest @EnumSource(Mercado.class)
    void custodiaEEstornosPreservamServicosFinanceirosPatrimonioESnapshots(Mercado mercado) {
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(REGISTRO.toInstant());
        when(clock.withZone(any(ZoneId.class))).thenAnswer(i -> Clock.fixed(clock.instant(), i.getArgument(0)));
        Fixture f = tx(e -> {
            for (String tabela : List.of("EstornoMovimentacaoCustodia", "MovimentacaoCustodia", "SnapshotCarteiraMoeda",
                    "SnapshotCarteira", "Operacao", "Acao", "Corretora", "Carteira"))
                e.createQuery("delete from " + tabela).executeUpdate();
            var c = new Carteira("Invariância", REGISTRO);
            var a = new Acao(mercado == Mercado.BRASIL ? "PETR4" : "AAPL", "Sintética", mercado,
                    mercado == Mercado.BRASIL ? Moeda.BRL : Moeda.USD, new BigDecimal("12"), REGISTRO);
            var origem = corretora("11111111000111"); var destino = corretora("22222222000122");
            for (Object item : List.of(c, a, origem, destino)) e.persist(item);
            e.persist(new Operacao(c, a, origem, TipoOperacao.COMPRA, new BigDecimal("100"), BigDecimal.TEN, DIA, 1, new BigDecimal("1000")));
            e.persist(new Operacao(c, a, null, TipoOperacao.COMPRA, new BigDecimal("100"), BigDecimal.TEN, DIA, 2, new BigDecimal("1000")));
            e.persist(new Operacao(c, a, origem, TipoOperacao.VENDA, new BigDecimal("20"), new BigDecimal("15"), DIA.plusDays(1), 1, new BigDecimal("300")));
            e.flush(); return new Fixture(c.getId(), a.getId(), origem.getId(), destino.getId(), a.getTicker());
        });
        var financeiroAntes = linhas("SELECT id,carteira_id,acao_id,corretora_id,tipo,quantidade,preco_unitario,data_operacao,ordem_no_dia,valor_total FROM operacao ORDER BY id");
        var posicoesAntes = posicoes.listarPorCarteira(f.carteira());
        var resumoAntes = resumos.consultar(f.carteira());
        var p = posicoesAntes.get(0);
        decimal("180", p.quantidadeAtual()); decimal("10", p.precoMedio()); decimal("1800", p.custoPosicao());
        decimal("2160", p.valorAtualPosicao()); decimal("360", p.resultadoNaoRealizado()); decimal("20", p.rentabilidadePercentual());
        var primeiro = snapshots.criar(f.carteira());
        decimal("2160", primeiro.patrimonios().get(0).patrimonioAtual());
        var snapshotAntes = linhas("SELECT id,carteira_id,data_hora_snapshot FROM snapshot_carteira ORDER BY id");
        var componentesAntes = linhas("SELECT id,snapshot_carteira_id,moeda,patrimonio_atual FROM snapshot_carteira_moeda ORDER BY id");

        var transferencia = transferencias.transferir(new TransferenciaCustodiaCommand(f.carteira(), f.ticker(), mercado,
                f.origem(), f.destino(), new BigDecimal("40"), DIA.plusDays(2)));
        assertNotNull(transferencia.getId());
        conferir(f, financeiroAntes, posicoesAntes, resumoAntes, snapshotAntes, componentesAntes);
        var atribuicao = atribuicoes.atribuir(new AtribuicaoCustodiaCommand(f.carteira(), f.ticker(), mercado,
                f.destino(), new BigDecimal("40"), DIA.plusDays(2)));
        assertNotNull(atribuicao.getId());
        conferir(f, financeiroAntes, posicoesAntes, resumoAntes, snapshotAntes, componentesAntes);
        for (Long id : List.of(transferencia.getId(), atribuicao.getId())) {
            var estorno = estornos.estornar(new EstornoCustodiaCommand(f.carteira(), id, "Regressão"));
            assertNotNull(estorno.getId());
            conferir(f, financeiroAntes, posicoesAntes, resumoAntes, snapshotAntes, componentesAntes);
        }
        when(clock.instant()).thenReturn(REGISTRO.plusSeconds(1).toInstant());
        var depois = snapshots.criar(f.carteira());
        assertNotEquals(primeiro.id(), depois.id()); assertEquals(primeiro.patrimonios(), depois.patrimonios());
        assertEquals(2L, tx(e -> e.createQuery("select count(m) from MovimentacaoCustodia m", Long.class).getSingleResult()).longValue());
        assertEquals(2L, tx(e -> e.createQuery("select count(m) from EstornoMovimentacaoCustodia m", Long.class).getSingleResult()).longValue());
    }
    void conferir(Fixture f, List<List<Object>> financeiro, List<com.projeto.dto.PosicaoResponse> posicao,
            com.projeto.dto.ResumoCarteiraResponse resumo, List<List<Object>> snapshot, List<List<Object>> componentes) {
        assertEquals(financeiro, linhas("SELECT id,carteira_id,acao_id,corretora_id,tipo,quantidade,preco_unitario,data_operacao,ordem_no_dia,valor_total FROM operacao ORDER BY id"));
        assertEquals(posicao, posicoes.listarPorCarteira(f.carteira())); assertEquals(resumo, resumos.consultar(f.carteira()));
        assertEquals(snapshot, linhas("SELECT id,carteira_id,data_hora_snapshot FROM snapshot_carteira ORDER BY id"));
        assertEquals(componentes, linhas("SELECT id,snapshot_carteira_id,moeda,patrimonio_atual FROM snapshot_carteira_moeda ORDER BY id"));
        var replay = tx(e -> new CalculadoraPosicao().reproduzir(operacoes.listarParaReplayQuantitativo(f.carteira(), f.acao())));
        assertTrue(replay.valido()); decimal("100", replay.resultadoRealizado());
    }
    List<List<Object>> linhas(String sql) {
        return tx(e -> { @SuppressWarnings("unchecked") List<Object[]> rows = e.createNativeQuery(sql).getResultList();
            return rows.stream().map(Arrays::asList).toList(); });
    }
    static void decimal(String esperado, BigDecimal atual) { assertEquals(0, new BigDecimal(esperado).compareTo(atual)); }
    static Corretora corretora(String cnpj) { return new Corretora(cnpj, "Sintética", null, null, null,
            "00000000", "Rua", "1", null, "Bairro", "Cidade", "SP", "ATIVA", REGISTRO); }
}
