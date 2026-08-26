package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Mercado;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Component
public class CalculadoraPosicao {

    public static final int ESCALA_INTERMEDIARIA = 24;
    public static final int ESCALA_SAIDA = 12;
    public static final int PRECISAO_PRECO_MEDIO = 25;
    public static final int PRECISAO_CUSTO = 38;
    public static final int PRECISAO_VALOR_ATUAL = 38;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    private static final int PRECISAO_OPERANDO = 19;
    private static final int ESCALA_OPERANDO = 6;
    private static final Comparator<Operacao> ORDEM_FINANCEIRA = Comparator
            .comparing(Operacao::getDataOperacao)
            .thenComparing(Operacao::getOrdemNoDia);

    public ResultadoReplay reproduzir(List<Operacao> operacoesOrdenadas) {
        return reproduzir(operacoesOrdenadas, true);
    }

    public ResultadoReplay validarQuantidade(List<Operacao> operacoesOrdenadas) {
        return reproduzir(operacoesOrdenadas, false);
    }

    public BigDecimal calcularValorAtual(BigDecimal quantidadeAtual, BigDecimal cotacaoAtual) {
        BigDecimal valorAtual = quantidadeAtual.multiply(cotacaoAtual)
                .setScale(ESCALA_SAIDA, RoundingMode.UNNECESSARY);
        if (valorAtual.precision() > PRECISAO_VALOR_ATUAL) {
            throw new ArithmeticException("Valor atual da posição excede a precisão aprovada");
        }
        return valorAtual;
    }

    private ResultadoReplay reproduzir(List<Operacao> operacoesOrdenadas, boolean calcularFinanceiro) {
        if (operacoesOrdenadas == null) {
            return falhaHistorico("Histórico de Operações ausente", null, null, null);
        }
        if (operacoesOrdenadas.isEmpty()) {
            return sucesso(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, calcularFinanceiro);
        }

        Operacao primeira = operacoesOrdenadas.get(0);
        Long carteiraId = carteiraId(primeira);
        Long acaoId = acaoId(primeira);
        Operacao anterior = null;
        BigDecimal quantidade = BigDecimal.ZERO;
        BigDecimal custo = BigDecimal.ZERO;
        BigDecimal precoMedio = BigDecimal.ZERO;

        for (Operacao operacao : operacoesOrdenadas) {
            ResultadoReplay falhaEstrutural = validarOperacao(
                    operacao,
                    anterior,
                    carteiraId,
                    acaoId,
                    calcularFinanceiro
            );
            if (falhaEstrutural != null) {
                return falhaEstrutural;
            }

            BigDecimal quantidadeOperacao = operacao.getQuantidade();
            if (operacao.getTipo() == TipoOperacao.COMPRA) {
                quantidade = quantidade.add(quantidadeOperacao);
                if (calcularFinanceiro) {
                    BigDecimal custoCompra = quantidadeOperacao.multiply(operacao.getPrecoUnitario());
                    custo = custo.add(custoCompra);
                    precoMedio = custo.divide(quantidade, ESCALA_INTERMEDIARIA, ARREDONDAMENTO);
                }
            } else {
                BigDecimal quantidadeDisponivel = quantidade;
                BigDecimal novaQuantidade = quantidade.subtract(quantidadeOperacao);
                if (novaQuantidade.signum() < 0) {
                    return falhaHistorico(
                            "Venda superior à quantidade cronologicamente disponível",
                            operacao,
                            quantidadeDisponivel,
                            quantidadeOperacao
                    );
                }

                if (calcularFinanceiro) {
                    if (novaQuantidade.signum() == 0) {
                        custo = BigDecimal.ZERO;
                        precoMedio = BigDecimal.ZERO;
                    } else {
                        custo = custo.multiply(novaQuantidade)
                                .divide(quantidade, ESCALA_INTERMEDIARIA, ARREDONDAMENTO);
                    }
                }
                quantidade = novaQuantidade;
            }

            if (calcularFinanceiro && !representavel(quantidade, precoMedio, custo)) {
                return falhaPrecisao("Estado financeiro excede a precisão aprovada", operacao);
            }
            anterior = operacao;
        }

        return sucesso(quantidade, precoMedio, custo, calcularFinanceiro);
    }

    private ResultadoReplay validarOperacao(
            Operacao operacao,
            Operacao anterior,
            Long carteiraId,
            Long acaoId,
            boolean calcularFinanceiro
    ) {
        if (operacao == null) {
            return falhaHistorico("Histórico contém Operação nula", null, null, null);
        }
        if (carteiraId == null || acaoId == null
                || !carteiraId.equals(carteiraId(operacao))
                || !acaoId.equals(acaoId(operacao))) {
            return falhaHistorico("Histórico mistura Carteiras ou Ações", operacao, null, null);
        }
        if (operacao.getDataOperacao() == null
                || operacao.getOrdemNoDia() == null
                || operacao.getOrdemNoDia() <= 0) {
            return falhaHistorico("Operação possui cronologia inválida", operacao, null, null);
        }
        if (anterior != null && ORDEM_FINANCEIRA.compare(anterior, operacao) >= 0) {
            return falhaHistorico(
                    "Histórico não respeita dataOperacao e ordemNoDia",
                    operacao,
                    null,
                    null
            );
        }
        if (operacao.getTipo() != TipoOperacao.COMPRA && operacao.getTipo() != TipoOperacao.VENDA) {
            return falhaHistorico("Operação possui tipo inválido", operacao, null, null);
        }
        if (!operandoValido(operacao.getQuantidade())) {
            return falhaHistorico("Operação possui quantidade inválida", operacao, null, null);
        }

        Acao acao = operacao.getAcao();
        if (acao.getMercado() == null
                || (acao.getMercado() == Mercado.BRASIL
                && operacao.getQuantidade().stripTrailingZeros().scale() > 0)) {
            return falhaHistorico("Quantidade incompatível com o mercado da Ação", operacao, null, null);
        }
        if (calcularFinanceiro && !operandoValido(operacao.getPrecoUnitario())) {
            return falhaHistorico("Operação possui preço unitário inválido", operacao, null, null);
        }
        return null;
    }

    private boolean operandoValido(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0 || valor.scale() > ESCALA_OPERANDO) {
            return false;
        }
        try {
            return valor.setScale(ESCALA_OPERANDO, RoundingMode.UNNECESSARY).precision()
                    <= PRECISAO_OPERANDO;
        } catch (ArithmeticException exception) {
            return false;
        }
    }

    private boolean representavel(BigDecimal quantidade, BigDecimal precoMedio, BigDecimal custo) {
        BigDecimal precoNormalizado = precoMedio.setScale(ESCALA_SAIDA, ARREDONDAMENTO);
        BigDecimal custoNormalizado = custo.setScale(ESCALA_SAIDA, ARREDONDAMENTO);
        boolean quantidadeRepresentavel;
        try {
            quantidadeRepresentavel = quantidade
                    .setScale(ESCALA_OPERANDO, RoundingMode.UNNECESSARY)
                    .precision() <= PRECISAO_OPERANDO;
        } catch (ArithmeticException exception) {
            quantidadeRepresentavel = false;
        }
        return quantidadeRepresentavel
                && precoNormalizado.precision() <= PRECISAO_PRECO_MEDIO
                && custoNormalizado.precision() <= PRECISAO_CUSTO;
    }

    private ResultadoReplay sucesso(
            BigDecimal quantidade,
            BigDecimal precoMedio,
            BigDecimal custo,
            boolean normalizarFinanceiro
    ) {
        BigDecimal precoSaida = normalizarFinanceiro
                ? precoMedio.setScale(ESCALA_SAIDA, ARREDONDAMENTO)
                : BigDecimal.ZERO;
        BigDecimal custoSaida = normalizarFinanceiro
                ? custo.setScale(ESCALA_SAIDA, ARREDONDAMENTO)
                : BigDecimal.ZERO;
        return new ResultadoReplay(
                new PosicaoCalculada(quantidade, precoSaida, custoSaida),
                null
        );
    }

    private ResultadoReplay falhaHistorico(
            String motivo,
            Operacao operacao,
            BigDecimal quantidadeDisponivel,
            BigDecimal quantidadeSolicitada
    ) {
        return new ResultadoReplay(
                null,
                new FalhaReplay(
                        TipoFalha.HISTORICO_INCONSISTENTE,
                        motivo,
                        operacao,
                        quantidadeDisponivel,
                        quantidadeSolicitada
                )
        );
    }

    private ResultadoReplay falhaPrecisao(String motivo, Operacao operacao) {
        return new ResultadoReplay(
                null,
                new FalhaReplay(TipoFalha.CALCULO_FORA_DA_PRECISAO, motivo, operacao, null, null)
        );
    }

    private Long carteiraId(Operacao operacao) {
        Carteira carteira = operacao == null ? null : operacao.getCarteira();
        return carteira == null ? null : carteira.getId();
    }

    private Long acaoId(Operacao operacao) {
        Acao acao = operacao == null ? null : operacao.getAcao();
        return acao == null ? null : acao.getId();
    }

    public record PosicaoCalculada(
            BigDecimal quantidadeAtual,
            BigDecimal precoMedio,
            BigDecimal custoPosicao
    ) {
    }

    public record ResultadoReplay(PosicaoCalculada posicao, FalhaReplay falha) {

        public boolean valido() {
            return falha == null;
        }
    }

    public record FalhaReplay(
            TipoFalha tipo,
            String motivo,
            Operacao operacao,
            BigDecimal quantidadeDisponivel,
            BigDecimal quantidadeSolicitada
    ) {
    }

    public enum TipoFalha {
        HISTORICO_INCONSISTENTE,
        CALCULO_FORA_DA_PRECISAO
    }
}
