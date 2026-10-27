package com.projeto.services;

import com.projeto.entities.Mercado;
import java.math.BigDecimal;
import java.util.UUID;

/** Retrato quantitativo imutável; não calcula saldos nem representa uma entidade persistente. */
public record EventoQuantitativo(Identidade identidade, long carteiraId, long acaoId, Mercado mercado,
        Natureza natureza, BigDecimal quantidade, PosicaoCronologicaQuantitativa posicao,
        Grupo grupoOrigem, Grupo grupoDestino, boolean neutralizado) {
    public enum Dominio { FINANCEIRO, CUSTODIA }
    public enum Natureza { COMPRA, VENDA, TRANSFERENCIA, ATRIBUICAO }

    /** Exatamente uma das alternativas: ID real ou token temporário fornecido pelo chamador. */
    public record Identidade(Dominio dominio, Long idPersistido, UUID tokenTemporario) {
        public Identidade {
            if (dominio == null || (idPersistido == null) == (tokenTemporario == null)
                    || (idPersistido != null && idPersistido <= 0)) {
                throw new IllegalArgumentException("Identidade deve ser persistida ou temporária, com domínio");
            }
        }
        public static Identidade persistida(Dominio dominio, long id) {
            return new Identidade(dominio, id, null);
        }
        public static Identidade temporaria(Dominio dominio, UUID token) {
            return new Identidade(dominio, null, token);
        }
    }

    /** O grupo nulo é uma chave explícita, sem corretora fictícia ou ID sentinela. */
    public record Grupo(Long corretoraId) {
        public Grupo {
            if (corretoraId != null && corretoraId <= 0) {
                throw new IllegalArgumentException("Corretora deve possuir ID positivo");
            }
        }
        public static Grupo semCorretora() { return new Grupo(null); }
        public static Grupo corretora(long id) { return new Grupo(id); }
    }

    public EventoQuantitativo {
        if (identidade == null || carteiraId <= 0 || acaoId <= 0 || mercado == null
                || natureza == null || posicao == null || grupoOrigem == null) {
            throw new IllegalArgumentException("Estrutura quantitativa incompleta");
        }
        if (quantidade == null || quantidade.signum() <= 0 || quantidade.scale() > 6
                || (long) quantidade.precision() + 6L - quantidade.scale() > 19
                || (mercado == Mercado.BRASIL && quantidade.stripTrailingZeros().scale() > 0)) {
            throw new IllegalArgumentException("Quantidade deve respeitar mercado e NUMERIC(19,6)");
        }
        boolean financeiro = natureza == Natureza.COMPRA || natureza == Natureza.VENDA;
        if (financeiro != posicao.financeira()
                || financeiro != (identidade.dominio() == Dominio.FINANCEIRO)) {
            throw new IllegalArgumentException("Domínio, natureza e posição incompatíveis");
        }
        if (financeiro) {
            if (grupoDestino != null || neutralizado) {
                throw new IllegalArgumentException("Financeiro possui um grupo e não é neutralizado por custódia");
            }
        } else {
            if (grupoDestino == null || grupoDestino.corretoraId() == null
                    || grupoOrigem.equals(grupoDestino)
                    || (natureza == Natureza.TRANSFERENCIA && grupoOrigem.corretoraId() == null)
                    || (natureza == Natureza.ATRIBUICAO && grupoOrigem.corretoraId() != null)
                    || (neutralizado && identidade.idPersistido() == null)) {
                throw new IllegalArgumentException("Direção ou neutralização de custódia inválida");
            }
        }
    }
}
