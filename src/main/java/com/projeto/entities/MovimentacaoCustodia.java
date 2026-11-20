package com.projeto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Auditoria sem operações públicas de edição. Recebe a posição definida pelo chamador.
 * Hidratação valida campos locais; referências devem ser carregadas para validação completa.
 */
@Entity
@Table(name = "movimentacao_custodia", uniqueConstraints = @UniqueConstraint(
        name = "uk_movimentacao_custodia_posicao", columnNames = {"carteira_id", "acao_id",
        "data_efetiva", "ordem_financeira_ancora", "posicao_numerador", "posicao_denominador"}))
public class MovimentacaoCustodia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carteira_id", nullable = false, updatable = false)
    private Carteira carteira;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acao_id", nullable = false, updatable = false)
    private Acao acao;

    @Enumerated(EnumType.STRING)
    @Column(name = "natureza", nullable = false, length = 20, updatable = false)
    private NaturezaMovimentacaoCustodia natureza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corretora_origem_id", updatable = false)
    private Corretora corretoraOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corretora_destino_id", nullable = false, updatable = false)
    private Corretora corretoraDestino;

    @Column(name = "quantidade", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal quantidade;

    @Column(name = "data_efetiva", nullable = false, updatable = false)
    private LocalDate dataEfetiva;

    @Column(name = "data_registro", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE", updatable = false)
    private OffsetDateTime dataRegistro;

    @Column(name = "ordem_financeira_ancora", nullable = false, updatable = false)
    private Integer ordemFinanceiraAncora;

    @Column(name = "posicao_numerador", nullable = false, length = 512, updatable = false)
    private String posicaoNumerador;

    @Column(name = "posicao_denominador", nullable = false, length = 512, updatable = false)
    private String posicaoDenominador;

    protected MovimentacaoCustodia() {
    }

    public MovimentacaoCustodia(Long id, Carteira carteira, Acao acao,
            NaturezaMovimentacaoCustodia natureza, Corretora corretoraOrigem,
            Corretora corretoraDestino, BigDecimal quantidade, LocalDate dataEfetiva,
            Integer ordemFinanceiraAncora, PosicaoRacionalCustodia posicaoRacional, Clock clock) {
        Mercado mercado = acao == null ? null : acao.getMercado();
        this.quantidade = validarEstrutura(id, carteira == null ? null : carteira.getId(),
                acao == null ? null : acao.getId(), mercado, natureza,
                corretoraOrigem == null ? null : corretoraOrigem.getId(),
                corretoraDestino == null ? null : corretoraDestino.getId(), quantidade, dataEfetiva,
                ordemFinanceiraAncora, posicaoRacional);
        if (natureza == NaturezaMovimentacaoCustodia.ATRIBUICAO && corretoraOrigem != null) {
            throw new IllegalArgumentException("ATRIBUICAO exige origem sem corretora");
        }
        this.dataRegistro = validarCriacao(dataEfetiva, mercado, clock);
        this.id = id;
        this.carteira = carteira;
        this.acao = acao;
        this.natureza = natureza;
        this.corretoraOrigem = corretoraOrigem;
        this.corretoraDestino = corretoraDestino;
        this.dataEfetiva = dataEfetiva;
        this.ordemFinanceiraAncora = ordemFinanceiraAncora;
        this.posicaoNumerador = posicaoRacional.getNumeradorTexto();
        this.posicaoDenominador = posicaoRacional.getDenominadorTexto();
    }

    /** Invariantes independentes do relógio; não resolve referências nem escolhe posição. */
    private static BigDecimal validarEstrutura(Long id, Long carteiraId, Long acaoId, Mercado mercado,
            NaturezaMovimentacaoCustodia natureza, Long corretoraOrigemId,
            Long corretoraDestinoId, BigDecimal quantidade, LocalDate dataEfetiva,
            Integer ordemFinanceiraAncora, PosicaoRacionalCustodia posicaoRacional) {
        if (id != null) validarId(id, "id");
        validarId(carteiraId, "carteiraId");
        validarId(acaoId, "acaoId");
        validarId(corretoraDestinoId, "corretoraDestinoId");
        if (mercado == null || natureza == null) {
            throw new IllegalArgumentException("Mercado e natureza são obrigatórios");
        }
        if (natureza == NaturezaMovimentacaoCustodia.TRANSFERENCIA) {
            validarId(corretoraOrigemId, "corretoraOrigemId");
            if (corretoraOrigemId.equals(corretoraDestinoId)) {
                throw new IllegalArgumentException("Origem e destino devem ser distintos");
            }
        } else if (corretoraOrigemId != null) {
            throw new IllegalArgumentException("ATRIBUICAO exige origem sem corretora");
        }
        if (dataEfetiva == null) {
            throw new IllegalArgumentException("Data efetiva é obrigatória");
        }
        if (ordemFinanceiraAncora == null || ordemFinanceiraAncora < 0) {
            throw new IllegalArgumentException("Âncora financeira deve ser não negativa");
        }
        if (posicaoRacional == null) {
            throw new IllegalArgumentException("Posição racional é obrigatória");
        }
        posicaoRacional.exigirPersistivel();
        return validarQuantidade(quantidade, mercado);
    }

    /** Somente criação: captura um instante e valida a data corrente do mercado. */
    private static OffsetDateTime validarCriacao(LocalDate dataEfetiva, Mercado mercado, Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Relógio é obrigatório");
        }
        OffsetDateTime registro = OffsetDateTime.now(clock);
        ZoneId zonaMercado = ZoneId.of(mercado == Mercado.BRASIL
                ? "America/Sao_Paulo" : "America/New_York");
        if (dataEfetiva.isAfter(registro.atZoneSameInstant(zonaMercado).toLocalDate())) {
            throw new IllegalArgumentException("Data efetiva não pode ser futura no mercado");
        }
        return registro;
    }

    static void validarId(Long id, String campo) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(campo + " deve ser positivo");
        }
    }

    private static BigDecimal validarQuantidade(BigDecimal quantidade, Mercado mercado) {
        validarLimitesQuantidade(quantidade);
        BigDecimal exata = quantidade.setScale(6, RoundingMode.UNNECESSARY);
        if (mercado == Mercado.BRASIL && exata.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Quantidade BRASIL deve ser matematicamente inteira");
        }
        return exata;
    }

    private static void validarLimitesQuantidade(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0 || quantidade.scale() > 6) {
            throw new IllegalArgumentException("Quantidade positiva com até seis casas é obrigatória");
        }
        // Para um valor positivo com escala <= 6, esta é a precisão após setScale(6).
        // Usar long evita overflow inclusive quando scale() é Integer.MIN_VALUE.
        long precisaoNormalizada = (long) quantidade.precision() + 6L - quantidade.scale();
        if (precisaoNormalizada > 19) {
            throw new IllegalArgumentException("Quantidade excede NUMERIC(19,6)");
        }
    }

    /** Não usa getters de associações, relógio ou atribuições a campos persistentes. */
    @PostLoad
    private void validarCamposHistoricosLocais() {
        validarId(id, "id");
        if (carteira == null || acao == null || corretoraDestino == null || natureza == null) {
            throw new IllegalArgumentException("Referências obrigatórias e natureza devem estar presentes");
        }
        if (natureza == NaturezaMovimentacaoCustodia.TRANSFERENCIA && corretoraOrigem == null) {
            throw new IllegalArgumentException("TRANSFERENCIA exige origem");
        }
        if (natureza == NaturezaMovimentacaoCustodia.ATRIBUICAO && corretoraOrigem != null) {
            throw new IllegalArgumentException("ATRIBUICAO exige origem sem corretora");
        }
        if (dataEfetiva == null || dataRegistro == null) {
            throw new IllegalArgumentException("Datas históricas devem estar presentes");
        }
        if (ordemFinanceiraAncora == null || ordemFinanceiraAncora < 0) {
            throw new IllegalArgumentException("Âncora financeira deve ser não negativa");
        }
        validarLimitesQuantidade(quantidade);
        PosicaoRacionalCustodia.reconstruirPersistida(posicaoNumerador, posicaoDenominador);
    }

    /** Deve ser chamada pelo futuro fluxo de consulta com referências carregadas no mesmo snapshot. */
    public void validarEstruturaHistorica() {
        validarCamposHistoricosLocais();
        exigirAssociacaoCarregada(carteira, "carteira");
        exigirAssociacaoCarregada(acao, "acao");
        exigirAssociacaoCarregada(corretoraDestino, "corretoraDestino");
        if (corretoraOrigem != null) exigirAssociacaoCarregada(corretoraOrigem, "corretoraOrigem");
        validarEstrutura(id, carteira.getId(), acao.getId(), acao.getMercado(), natureza,
                corretoraOrigem == null ? null : corretoraOrigem.getId(), corretoraDestino.getId(),
                quantidade, dataEfetiva, ordemFinanceiraAncora,
                PosicaoRacionalCustodia.reconstruirPersistida(posicaoNumerador, posicaoDenominador));
    }

    static void exigirAssociacaoCarregada(Object associacao, String campo) {
        if (!Hibernate.isInitialized(associacao)) {
            throw new IllegalStateException(campo + " deve estar carregada para validação histórica");
        }
    }

    /** Situação depende de resultado explícito de consulta, nunca do ciclo de vida ORM. */
    public SituacaoMovimentacaoCustodia getSituacao(VinculoEstornoConsultado consulta) {
        if (consulta == null) {
            throw new IllegalArgumentException("Resultado de consulta do vínculo é obrigatório");
        }
        if (!consulta.getMovimentacaoOriginalId().equals(id)) {
            throw new IllegalArgumentException("Consulta de vínculo não pertence à movimentação original");
        }
        EstornoMovimentacaoCustodia estorno = consulta.getEstorno();
        if (estorno == null) return SituacaoMovimentacaoCustodia.ATIVA;
        exigirAssociacaoCarregada(estorno, "estorno");
        estorno.validarEstruturaHistorica();
        MovimentacaoCustodia original = estorno.getMovimentacaoOriginal();
        if (original != this && (id == null || !id.equals(original.getId())
                || !getCarteiraId().equals(original.getCarteiraId()) || !getAcaoId().equals(original.getAcaoId()))) {
            throw new IllegalArgumentException("Estorno não pertence à movimentação original");
        }
        return SituacaoMovimentacaoCustodia.ESTORNADA;
    }

    public Long getId() { return id; }
    public Carteira getCarteira() { return carteira; }
    public Acao getAcao() { return acao; }
    public Corretora getCorretoraOrigem() { return corretoraOrigem; }
    public Corretora getCorretoraDestino() { return corretoraDestino; }
    public Long getCarteiraId() { return carteira.getId(); }
    public Long getAcaoId() { return acao.getId(); }
    public Mercado getMercado() { return acao.getMercado(); }
    public NaturezaMovimentacaoCustodia getNatureza() { return natureza; }
    public Long getCorretoraOrigemId() { return corretoraOrigem == null ? null : corretoraOrigem.getId(); }
    public Long getCorretoraDestinoId() { return corretoraDestino.getId(); }
    public BigDecimal getQuantidade() { return quantidade; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public OffsetDateTime getDataRegistro() { return dataRegistro; }
    public Integer getOrdemFinanceiraAncora() { return ordemFinanceiraAncora; }
    public PosicaoRacionalCustodia getPosicaoRacional() {
        return PosicaoRacionalCustodia.reconstruirPersistida(posicaoNumerador, posicaoDenominador);
    }
}
