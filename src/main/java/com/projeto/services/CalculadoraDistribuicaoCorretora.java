package com.projeto.services;

import com.projeto.entities.Corretora;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure projection of a caller-owned consistent operation snapshot; no database access. */
@Component
public final class CalculadoraDistribuicaoCorretora {
    private static final Comparator<Operacao> CRONOLOGIA = Comparator
            .comparing(Operacao::getDataOperacao)
            .thenComparing(Operacao::getOrdemNoDia).thenComparing(Operacao::getId);
    private static final Comparator<DiagnosticoCorretora> ORDEM_GRUPO = Comparator
            .comparing((DiagnosticoCorretora d) -> d.corretoraId() == null)
            .thenComparing(DiagnosticoCorretora::nome)
            .thenComparing(DiagnosticoCorretora::corretoraId, Comparator.nullsLast(Comparator.naturalOrder()));
    private final CalculadoraPosicao consolidado;

    public CalculadoraDistribuicaoCorretora(CalculadoraPosicao consolidado) {
        this.consolidado = Objects.requireNonNull(consolidado);
    }

    public Distribuicao calcular(Long carteiraId, List<Operacao> operacoes) {
        if (carteiraId == null || carteiraId <= 0 || operacoes == null) {
            throw new IllegalArgumentException("Carteira e snapshot de operacoes obrigatorios");
        }
        Map<Long, List<Operacao>> porAcao = new LinkedHashMap<>();
        for (Operacao op : operacoes) {
            if (op == null || op.getCarteira() == null || op.getCarteira().getId() == null) {
                throw historicoInvalido(carteiraId);
            }
            if (!carteiraId.equals(op.getCarteira().getId())) continue;
            if (op.getId() == null || op.getId() <= 0 || op.getAcao() == null
                    || op.getAcao().getId() == null || op.getAcao().getId() <= 0
                    || op.getDataOperacao() == null || op.getOrdemNoDia() == null) {
                throw historicoInvalido(carteiraId);
            }
            porAcao.computeIfAbsent(op.getAcao().getId(), ignored -> new ArrayList<>()).add(op);
        }
        List<Posicao> posicoes = new ArrayList<>();
        for (List<Operacao> historico : porAcao.values()) {
            historico.sort(CRONOLOGIA);
            var replay = consolidado.validarQuantidade(historico);
            if (!replay.valido()) throw historicoInvalido(carteiraId);
            BigDecimal quantidade = replay.posicao().quantidadeAtual();
            if (quantidade.signum() == 0) continue;

            var acao = historico.get(0).getAcao();
            if (acao.getTicker() == null || acao.getTicker().isBlank()
                    || acao.getMoeda() != (acao.getMercado() == Mercado.BRASIL ? Moeda.BRL : Moeda.USD)) {
                throw historicoInvalido(carteiraId);
            }
            Map<Long, Grupo> grupos = new LinkedHashMap<>();
            for (Operacao op : historico) {
                Corretora corretora = op.getCorretora();
                Long id = corretora == null ? null : corretora.getId();
                if (corretora != null && (id == null || id <= 0)) throw historicoInvalido(carteiraId);
                Grupo grupo = grupos.computeIfAbsent(id, ignored -> new Grupo(id, nome(corretora)));
                grupo.aplicar(op);
            }
            List<DiagnosticoCorretora> diagnosticos = grupos.values().stream()
                    .map(Grupo::diagnostico).sorted(ORDEM_GRUPO).toList();
            List<QuantidadeCorretora> visiveis = diagnosticos.stream()
                    .filter(d -> d.quantidadeFinal().signum() != 0)
                    .map(d -> new QuantidadeCorretora(d.corretoraId(), d.nome(), d.quantidadeFinal())).toList();
            Reconciliacao reconciliacao = reconciliar(quantidade, diagnosticos);
            posicoes.add(new Posicao(acao.getId(), acao.getTicker(), acao.getMercado(), acao.getMoeda(),
                    quantidade, reconciliacao.quantidadeAtribuidaTotal(), reconciliacao.reconciliada(),
                    reconciliacao.diagnostico(), visiveis, diagnosticos));
        }
        posicoes.sort(Comparator.comparingInt((Posicao p) -> p.mercado() == Mercado.BRASIL ? 0 : 1)
                .thenComparing(Posicao::ticker).thenComparing(Posicao::acaoId));
        return new Distribuicao(carteiraId, posicoes);
    }

    static Reconciliacao reconciliar(BigDecimal quantidade, List<DiagnosticoCorretora> grupos) {
        BigDecimal total = grupos.stream().map(DiagnosticoCorretora::quantidadeFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean igual = quantidade.compareTo(total) == 0;
        return new Reconciliacao(total, igual, igual ? null
                : new DiagnosticoReconciliacao("DIVERGENCIA_QUANTIDADES"));
    }

    private static String nome(Corretora corretora) {
        if (corretora == null) return "Sem corretora informada";
        if (corretora.getNomeFantasia() != null && !corretora.getNomeFantasia().isBlank()) {
            return corretora.getNomeFantasia().trim();
        }
        if (corretora.getRazaoSocial() != null && !corretora.getRazaoSocial().isBlank()) {
            return corretora.getRazaoSocial().trim();
        }
        return "Corretora #" + corretora.getId();
    }

    private static ApiException historicoInvalido(Long carteiraId) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE,
                "Historico de Operacoes inconsistente", Map.of("carteiraId", carteiraId));
    }

    private static final class Grupo {
        private final Long id;
        private final String nome;
        private BigDecimal quantidade = BigDecimal.ZERO;
        private PrimeiroDeficit primeiro;

        private Grupo(Long id, String nome) { this.id = id; this.nome = nome; }

        private void aplicar(Operacao op) {
            quantidade = op.getTipo() == TipoOperacao.COMPRA
                    ? quantidade.add(op.getQuantidade()) : quantidade.subtract(op.getQuantidade());
            if (primeiro == null && quantidade.signum() < 0) {
                primeiro = new PrimeiroDeficit(op.getId(), op.getDataOperacao(), op.getOrdemNoDia(), quantidade);
            }
        }

        private DiagnosticoCorretora diagnostico() {
            return new DiagnosticoCorretora(id, nome, quantidade, quantidade.signum() < 0,
                    primeiro != null, primeiro);
        }
    }

    public record Distribuicao(Long carteiraId, List<Posicao> posicoes) {
        public Distribuicao { posicoes = List.copyOf(posicoes); }
    }
    public record Posicao(Long acaoId, String ticker, Mercado mercado, Moeda moeda,
                          BigDecimal quantidadeConsolidada, BigDecimal quantidadeAtribuidaTotal,
                          boolean reconciliada, DiagnosticoReconciliacao diagnosticoReconciliacao,
                          List<QuantidadeCorretora> corretoras, List<DiagnosticoCorretora> diagnosticosCorretoras) {
        public Posicao {
            corretoras = List.copyOf(corretoras);
            diagnosticosCorretoras = List.copyOf(diagnosticosCorretoras);
        }
    }
    public record QuantidadeCorretora(Long corretoraId, String nome, BigDecimal quantidadeLiquida) { }
    public record DiagnosticoCorretora(Long corretoraId, String nome, BigDecimal quantidadeFinal,
                                     boolean saldoNegativoAtual, boolean possuiDeficitHistorico,
                                     PrimeiroDeficit primeiroDeficit) { }
    public record PrimeiroDeficit(Long operacaoId, LocalDate dataOperacao, Integer ordemNoDia,
                                 BigDecimal saldoAposOperacao) { }
    public record DiagnosticoReconciliacao(String codigo) { }
    record Reconciliacao(BigDecimal quantidadeAtribuidaTotal, boolean reconciliada,
                         DiagnosticoReconciliacao diagnostico) { }
}
