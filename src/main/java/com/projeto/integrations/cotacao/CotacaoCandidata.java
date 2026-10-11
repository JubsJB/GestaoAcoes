package com.projeto.integrations.cotacao;

import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.ReferenciaTipoCotacao;
import com.projeto.integrations.ExternalApiErrorMapper;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;

/** Obtenção válida interna, sem decisão de aceitação, persistência ou comparação temporal. */
public record CotacaoCandidata(
        String ticker, String nomeEmpresa, Moeda moeda, BigDecimal cotacao,
        ReferenciaTipoCotacao referenciaTipo, OffsetDateTime referenciaInstante,
        LocalDate referenciaData, OffsetDateTime cotacaoObtidaEm
) {
    public CotacaoCandidata {
        if (ticker == null || ticker.isBlank() || nomeEmpresa == null || nomeEmpresa.isBlank()
                || moeda == null || referenciaTipo == null || cotacaoObtidaEm == null) {
            throw new IllegalArgumentException("Candidata deve representar uma obtenção válida completa");
        }
        boolean referenciaValida = switch (referenciaTipo) {
            case DATA -> referenciaData != null && referenciaInstante == null;
            case INSTANTE -> referenciaInstante != null && referenciaData == null;
            case DESCONHECIDA -> referenciaData == null && referenciaInstante == null;
        };
        if (!referenciaValida) throw new IllegalArgumentException("Referência temporal incoerente");
        cotacao = precoValido(cotacao);
        if (referenciaData != null && (referenciaData.getYear() < 1 || referenciaData.getYear() > 9999)) {
            throw erro(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA, "Data fora do calendário civil 0001–9999");
        }
        referenciaInstante = instantePersistivel(referenciaInstante);
        cotacaoObtidaEm = instantePersistivel(cotacaoObtidaEm);
        if (referenciaInstante != null && (referenciaInstante.toInstant().isBefore(java.time.Instant.EPOCH)
                || referenciaInstante.toInstant().isAfter(cotacaoObtidaEm.toInstant()))) {
            throw erro(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA,
                    "Referência deve estar entre 1970 e a obtenção da própria candidata");
        }
    }

    private static OffsetDateTime instantePersistivel(OffsetDateTime instante) {
        if (instante == null) return null;
        if (instante.getNano() % 1000 != 0) {
            throw erro(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA,
                    "Instante não exatamente representável em microssegundos");
        }
        return instante.withOffsetSameInstant(ZoneOffset.UTC);
    }

    static CotacaoCandidata obtidaDe(CotacaoData data, String tickerSolicitado,
                                    Mercado mercado, boolean cadastro, Clock clock) {
        String provider = mercado == Mercado.BRASIL ? "BRAPI" : "Alpha Vantage";
        if (data == null || data.ticker() == null || data.ticker().isBlank()
                || data.nomeEmpresa() == null || data.nomeEmpresa().isBlank()) {
            throw erro(ErrorCodes.DADOS_EXTERNOS_INCOMPLETOS, "Dados obrigatórios ausentes");
        }
        String ticker = data.ticker().trim().toUpperCase(Locale.ROOT);
        String nome = data.nomeEmpresa().trim();
        Moeda moeda = mercado == Mercado.BRASIL ? Moeda.BRL : Moeda.USD;
        if (ticker.length() > 30 || nome.length() > 255 || data.moeda() == null
                || !moeda.name().equals(data.moeda().trim().toUpperCase(Locale.ROOT))) {
            throw ExternalApiErrorMapper.invalidResponse(provider);
        }
        if (!tickerSolicitado.equals(ticker)) {
            if (mercado == Mercado.BRASIL && data.tickerAlteradoExplicitamente()) {
                if (!cadastro) {
                    throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.TICKER_CANONICO_DIVERGENTE,
                            "O provider informou um ticker canônico diferente do persistido",
                            Map.of("tickerPersistido", tickerSolicitado, "tickerCanonicoRetornado", ticker));
                }
            } else {
                throw ExternalApiErrorMapper.invalidResponse(provider);
            }
        }
        BigDecimal preco = precoValido(data.cotacao());
        if (data.referenciaInvalida()) {
            throw erro(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA, "Referência de mercado inválida");
        }
        ReferenciaTipoCotacao tipo = data.referenciaData() != null ? ReferenciaTipoCotacao.DATA
                : data.dataHoraCotacao() != null ? ReferenciaTipoCotacao.INSTANTE
                : ReferenciaTipoCotacao.DESCONHECIDA;
        if (tipo == ReferenciaTipoCotacao.DESCONHECIDA && !cadastro) {
            throw erro(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA, "Atualização exige referência de mercado");
        }
        if (data.referenciaData() != null && data.dataHoraCotacao() != null) {
            throw erro(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA, "Referências de mercado incompatíveis");
        }
        // O relógio da aplicação é lido somente depois de validar a resposta real.
        return new CotacaoCandidata(ticker, nome, moeda, preco, tipo,
                data.dataHoraCotacao(), data.referenciaData(), OffsetDateTime.now(clock));
    }

    private static BigDecimal precoValido(BigDecimal preco) {
        if (preco == null || preco.signum() <= 0) {
            throw erro(ErrorCodes.COTACAO_INDISPONIVEL, "Cotação indisponível");
        }
        try {
            BigDecimal exato = preco.setScale(6, RoundingMode.UNNECESSARY);
            if (exato.precision() > 19) throw new ArithmeticException();
            return exato;
        } catch (ArithmeticException exception) {
            throw erro(ErrorCodes.COTACAO_FORA_DA_PRECISAO, "Cotação fora da precisão suportada");
        }
    }

    private static ApiException erro(String codigo, String mensagem) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, codigo, mensagem);
    }
}
