package com.projeto.services;

import com.projeto.dto.AcaoResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.services.exceptions.ApiException;

/** Resultado interno, independente do futuro contrato HTTP do lote. */
public record ResultadoAtualizacaoCotacao(Long acaoId, String ticker, String nomeEmpresa,
        Mercado mercado, Moeda moeda, RegraDecisaoCotacao.Resultado resultado, String motivo,
        Origem origem, AplicacaoCotacaoService.EstadoCotacao cotacao, ApiException falha) {
    public enum Origem { CONSULTA, REUSO, COOLDOWN }

    public ResultadoAtualizacaoCotacao {
        if (acaoId == null || ticker == null || ticker.isBlank() || nomeEmpresa == null || nomeEmpresa.isBlank()
                || mercado == null || moeda == null || resultado == null || origem == null
                || motivo == null || motivo.isBlank() || cotacao == null) {
            throw new IllegalArgumentException("Identificação, resultado, origem, motivo e snapshot obrigatórios");
        }
        if ((resultado == RegraDecisaoCotacao.Resultado.FALHA) != (falha != null)) {
            throw new IllegalArgumentException("Somente FALHA exige erro");
        }
        boolean compativel = switch (origem) {
            case CONSULTA -> switch (resultado) {
                case ATUALIZADA -> motivo.equals("REFERENCIA_INICIAL_ESTABELECIDA") || motivo.equals("REFERENCIA_POSTERIOR");
                case SEM_NOVA_COTACAO -> motivo.equals("MESMA_OBSERVACAO") || motivo.equals("REFERENCIA_ANTERIOR");
                case FALHA -> !motivo.equals("REUSO") && !motivo.equals("COOLDOWN_SUCESSO")
                        && !motivo.equals("COOLDOWN_FALHA");
            };
            case REUSO -> resultado == RegraDecisaoCotacao.Resultado.SEM_NOVA_COTACAO && motivo.equals("REUSO");
            case COOLDOWN -> resultado == RegraDecisaoCotacao.Resultado.SEM_NOVA_COTACAO
                    ? motivo.equals("COOLDOWN_SUCESSO")
                    : resultado == RegraDecisaoCotacao.Resultado.FALHA && motivo.equals("COOLDOWN_FALHA");
        };
        if (!compativel) throw new IllegalArgumentException("Classificação, origem e motivo incompatíveis");
    }

    ResultadoAtualizacaoCotacao comSnapshot(AplicacaoCotacaoService.EstadoCotacao estado) {
        if (resultado != RegraDecisaoCotacao.Resultado.FALHA || origem != Origem.CONSULTA) {
            throw new IllegalArgumentException("Somente falha de consulta admite leitura posterior de apresentação");
        }
        return new ResultadoAtualizacaoCotacao(acaoId, ticker, nomeEmpresa, mercado, moeda,
                resultado, motivo, origem, estado, falha);
    }

    static AplicacaoCotacaoService.EstadoCotacao estado(Acao acao) {
        return new AplicacaoCotacaoService.EstadoCotacao(acao.getCotacaoAtual(), acao.getReferenciaTipo(),
                acao.getReferenciaInstante(), acao.getReferenciaData(), acao.getCotacaoObtidaEm(),
                acao.getDataHoraCotacao());
    }

    static ResultadoAtualizacaoCotacao de(Acao acao, RegraDecisaoCotacao.Resultado resultado,
            String motivo, Origem origem, AplicacaoCotacaoService.EstadoCotacao estado, ApiException falha) {
        return new ResultadoAtualizacaoCotacao(acao.getId(), acao.getTicker(), acao.getNomeEmpresa(),
                acao.getMercado(), acao.getMoeda(), resultado, motivo, origem, estado, falha);
    }

    ResultadoAtualizacaoCotacao emCooldown() {
        if (falha != null || origem != Origem.CONSULTA) {
            throw new IllegalArgumentException("Cooldown de sucesso exige tentativa de consulta bem-sucedida");
        }
        return new ResultadoAtualizacaoCotacao(acaoId, ticker, nomeEmpresa, mercado, moeda,
                RegraDecisaoCotacao.Resultado.SEM_NOVA_COTACAO, "COOLDOWN_SUCESSO", Origem.COOLDOWN,
                cotacao, null);
    }

    AcaoResponse respostaIndividual() {
        if (falha != null) throw falha;
        return new AcaoResponse(acaoId, ticker, nomeEmpresa, mercado, moeda, cotacao.preco(), cotacao.legado(),
                new com.projeto.dto.ReferenciaMercadoCotacaoResponse(cotacao.tipo(), cotacao.instante(), cotacao.data()),
                cotacao.obtencao());
    }
}
