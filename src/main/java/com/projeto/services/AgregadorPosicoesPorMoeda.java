package com.projeto.services;

import com.projeto.dto.PosicaoResponse;
import com.projeto.entities.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AgregadorPosicoesPorMoeda {

    private static final int ESCALA = 12;
    private static final int PRECISAO = 38;
    private static final Comparator<Moeda> ORDEM_MOEDA = Comparator.comparing(Enum::name);

    public List<TotaisPorMoeda> agregar(Collection<PosicaoResponse> posicoes) {
        Map<Moeda, Acumulados> acumulados = new EnumMap<>(Moeda.class);
        for (PosicaoResponse posicao : posicoes) {
            acumulados.computeIfAbsent(posicao.moeda(), ignored -> new Acumulados())
                    .adicionar(posicao);
        }

        List<TotaisPorMoeda> totais = new ArrayList<>(acumulados.size());
        acumulados.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(ORDEM_MOEDA))
                .forEach(entry -> totais.add(normalizar(entry.getKey(), entry.getValue())));
        return List.copyOf(totais);
    }

    private TotaisPorMoeda normalizar(Moeda moeda, Acumulados acumulados) {
        return new TotaisPorMoeda(
                moeda,
                normalizar(moeda, Indicador.CUSTO_TOTAL_POSICOES, acumulados.custoTotalPosicoes),
                normalizar(moeda, Indicador.PATRIMONIO_ATUAL, acumulados.patrimonioAtual),
                normalizar(
                        moeda,
                        Indicador.RESULTADO_NAO_REALIZADO_TOTAL,
                        acumulados.resultadoNaoRealizadoTotal
                )
        );
    }

    private BigDecimal normalizar(Moeda moeda, Indicador indicador, BigDecimal acumulado) {
        try {
            BigDecimal normalizado = acumulado.setScale(ESCALA, RoundingMode.UNNECESSARY);
            if (normalizado.precision() > PRECISAO) {
                throw new ArithmeticException("Acumulado excede a precisão máxima 38");
            }
            return normalizado;
        } catch (ArithmeticException exception) {
            throw new FalhaAgregacaoException(moeda, indicador, exception);
        }
    }

    private static final class Acumulados {

        private BigDecimal custoTotalPosicoes = BigDecimal.ZERO;
        private BigDecimal patrimonioAtual = BigDecimal.ZERO;
        private BigDecimal resultadoNaoRealizadoTotal = BigDecimal.ZERO;

        private void adicionar(PosicaoResponse posicao) {
            custoTotalPosicoes = custoTotalPosicoes.add(posicao.custoPosicao());
            patrimonioAtual = patrimonioAtual.add(posicao.valorAtualPosicao());
            resultadoNaoRealizadoTotal = resultadoNaoRealizadoTotal.add(
                    posicao.resultadoNaoRealizado()
            );
        }
    }

    public record TotaisPorMoeda(
            Moeda moeda,
            BigDecimal custoTotalPosicoes,
            BigDecimal patrimonioAtual,
            BigDecimal resultadoNaoRealizadoTotal
    ) {
    }

    public enum Indicador {
        CUSTO_TOTAL_POSICOES,
        PATRIMONIO_ATUAL,
        RESULTADO_NAO_REALIZADO_TOTAL
    }

    public static final class FalhaAgregacaoException extends ArithmeticException {

        private final Moeda moeda;
        private final Indicador indicador;

        private FalhaAgregacaoException(
                Moeda moeda,
                Indicador indicador,
                ArithmeticException cause
        ) {
            super(cause.getMessage());
            this.moeda = moeda;
            this.indicador = indicador;
            initCause(cause);
        }

        public Moeda moeda() {
            return moeda;
        }

        public Indicador indicador() {
            return indicador;
        }
    }
}
