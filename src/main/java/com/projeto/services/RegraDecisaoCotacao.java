package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.entities.ReferenciaTipoCotacao;
import com.projeto.integrations.cotacao.CotacaoCandidata;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Decisão pura sobre uma nova obtenção válida para a identidade informada.
 * ATUALIZADA é a classificação pretendida: somente persistência futura poderá confirmá-la.
 * Reuso e cooldown não devem ser representados como candidatas nesta regra.
 */
public final class RegraDecisaoCotacao {
    private RegraDecisaoCotacao() { }

    public enum Resultado { ATUALIZADA, SEM_NOVA_COTACAO, FALHA }

    /** Motivos internos; não define novos códigos HTTP ou contratos de response. */
    public enum Motivo {
        REFERENCIA_INICIAL_ESTABELECIDA, REFERENCIA_POSTERIOR, MESMA_OBSERVACAO,
        REFERENCIA_ANTERIOR, CONFLITO_NA_MESMA_REFERENCIA,
        TIPO_REFERENCIA_INCOMPATIVEL, REFERENCIA_TEMPORAL_INVALIDA
    }

    public enum Efeito {
        SUBSTITUIR_OBSERVACAO, AVANCAR_SOMENTE_OBTENCAO, PRESERVAR_OBSERVACAO
    }

    /** Resultado imutável; a candidata vinculada fornece os dados da substituição futura. */
    public record Decisao(Resultado resultado, Motivo motivo, Efeito efeito,
                          OffsetDateTime cotacaoObtidaEmResultante,
                          CotacaoCandidata candidataParaSubstituicao) {
        public Decisao {
            if (resultado == null || motivo == null || efeito == null) {
                throw new IllegalArgumentException("Resultado, motivo e efeito obrigatórios");
            }
            boolean compativel = switch (resultado) {
                case ATUALIZADA -> efeito == Efeito.SUBSTITUIR_OBSERVACAO
                        && (motivo == Motivo.REFERENCIA_INICIAL_ESTABELECIDA
                        || motivo == Motivo.REFERENCIA_POSTERIOR);
                case SEM_NOVA_COTACAO -> (efeito == Efeito.PRESERVAR_OBSERVACAO
                        && (motivo == Motivo.REFERENCIA_ANTERIOR || motivo == Motivo.MESMA_OBSERVACAO))
                        || (efeito == Efeito.AVANCAR_SOMENTE_OBTENCAO && motivo == Motivo.MESMA_OBSERVACAO);
                case FALHA -> efeito == Efeito.PRESERVAR_OBSERVACAO
                        && (motivo == Motivo.CONFLITO_NA_MESMA_REFERENCIA
                        || motivo == Motivo.TIPO_REFERENCIA_INCOMPATIVEL
                        || motivo == Motivo.REFERENCIA_TEMPORAL_INVALIDA);
            };
            if (!compativel) {
                throw new IllegalArgumentException("Combinação de resultado, motivo e efeito incompatível");
            }
            if (efeito == Efeito.SUBSTITUIR_OBSERVACAO) {
                if (candidataParaSubstituicao == null || cotacaoObtidaEmResultante == null
                        || candidataParaSubstituicao.referenciaTipo() == ReferenciaTipoCotacao.DESCONHECIDA
                        || !cotacaoObtidaEmResultante.isEqual(candidataParaSubstituicao.cotacaoObtidaEm())) {
                    throw new IllegalArgumentException("Substituição exige candidata confiável e sua própria obtenção");
                }
            } else {
                if (candidataParaSubstituicao != null) {
                    throw new IllegalArgumentException("Candidata de substituição incompatível com o efeito");
                }
                if (efeito == Efeito.AVANCAR_SOMENTE_OBTENCAO && cotacaoObtidaEmResultante == null) {
                    throw new IllegalArgumentException("Avanço exige obtenção válida");
                }
            }
        }
    }

    public static Decisao decidir(Acao atual, CotacaoCandidata candidata) {
        Objects.requireNonNull(atual, "Estado atual obrigatório");
        Objects.requireNonNull(candidata, "Candidata obrigatória");
        if (!Objects.equals(atual.getTicker(), candidata.ticker())
                || atual.getMoeda() != candidata.moeda()) {
            throw new IllegalArgumentException("Candidata deve corresponder à identidade atual");
        }
        if (candidata.referenciaTipo() == ReferenciaTipoCotacao.DESCONHECIDA) {
            return preservar(atual, Resultado.FALHA, Motivo.REFERENCIA_TEMPORAL_INVALIDA);
        }
        if (atual.getReferenciaTipo() == ReferenciaTipoCotacao.DESCONHECIDA) {
            return substituir(candidata, Motivo.REFERENCIA_INICIAL_ESTABELECIDA);
        }
        if (atual.getReferenciaTipo() != candidata.referenciaTipo()) {
            return preservar(atual, Resultado.FALHA, Motivo.TIPO_REFERENCIA_INCOMPATIVEL);
        }
        int ordem = switch (candidata.referenciaTipo()) {
            case DATA -> candidata.referenciaData().compareTo(atual.getReferenciaData());
            case INSTANTE -> candidata.referenciaInstante().toInstant()
                    .compareTo(atual.getReferenciaInstante().toInstant());
            case DESCONHECIDA -> throw new IllegalStateException("Referência desconhecida já rejeitada");
        };
        if (ordem > 0) return substituir(candidata, Motivo.REFERENCIA_POSTERIOR);
        if (ordem < 0) return preservar(atual, Resultado.SEM_NOVA_COTACAO, Motivo.REFERENCIA_ANTERIOR);
        if (candidata.cotacao().compareTo(atual.getCotacaoAtual()) != 0) {
            return preservar(atual, Resultado.FALHA, Motivo.CONFLITO_NA_MESMA_REFERENCIA);
        }
        OffsetDateTime obtencao = atual.getCotacaoObtidaEm();
        if (obtencao == null || candidata.cotacaoObtidaEm().isAfter(obtencao)) {
            return new Decisao(Resultado.SEM_NOVA_COTACAO, Motivo.MESMA_OBSERVACAO,
                    Efeito.AVANCAR_SOMENTE_OBTENCAO, candidata.cotacaoObtidaEm(), null);
        }
        return preservar(atual, Resultado.SEM_NOVA_COTACAO, Motivo.MESMA_OBSERVACAO);
    }

    private static Decisao substituir(CotacaoCandidata candidata, Motivo motivo) {
        // A obtenção pertence à nova observação, mesmo quando menor que a da observação substituída.
        return new Decisao(Resultado.ATUALIZADA, motivo, Efeito.SUBSTITUIR_OBSERVACAO,
                candidata.cotacaoObtidaEm(), candidata);
    }

    private static Decisao preservar(Acao atual, Resultado resultado, Motivo motivo) {
        return new Decisao(resultado, motivo, Efeito.PRESERVAR_OBSERVACAO, atual.getCotacaoObtidaEm(), null);
    }
}
