package com.projeto.services;

import com.projeto.entities.Mercado;
import com.projeto.entities.NaturezaMovimentacaoCustodia;
import com.projeto.entities.PosicaoRacionalCustodia;
import com.projeto.entities.TipoOperacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static com.projeto.services.EventoQuantitativo.*;

/**
 * Converte retratos escalares já carregados, nunca entidades/proxies JPA. O chamador garante
 * leitura completa e consistente dos vínculos, inclusive ausência verificada; esta classe
 * não certifica commit. Registro do estorno não participa da cronologia quantitativa.
 */
public final class NormalizadorEventosQuantitativos {
    public record Financeiro(Identidade identidade, long carteiraId, long acaoId, Mercado mercado,
            TipoOperacao tipo, BigDecimal quantidade, LocalDate dataOperacao,
            int ordemNoDia, Grupo grupo) { }

    /** estornoId nulo significa ausência verificada, não vínculo ainda não consultado. */
    public record VinculoEstorno(Identidade original, Long estornoId) {
        public VinculoEstorno {
            if (original == null || original.dominio() != Dominio.CUSTODIA
                    || (estornoId != null && (estornoId <= 0 || original.idPersistido() == null))) {
                throw new IllegalArgumentException("Vínculo de estorno inválido");
            }
        }
        public static VinculoEstorno ausenciaVerificada(Identidade original) {
            return new VinculoEstorno(original, null);
        }
        public static VinculoEstorno presente(Identidade original, long estornoId) {
            return new VinculoEstorno(original, estornoId);
        }
    }

    public record Custodia(Identidade identidade, long carteiraId, long acaoId, Mercado mercado,
            NaturezaMovimentacaoCustodia natureza, BigDecimal quantidade, LocalDate dataEfetiva,
            int ancoraFinanceira, PosicaoRacionalCustodia fracao, Grupo origem, Grupo destino,
            VinculoEstorno vinculo) { }

    /** Normaliza somente um contexto carteira/ação; misturas são erros, nunca filtros silenciosos. */
    public List<EventoQuantitativo> normalizar(long carteiraId, long acaoId,
            List<Financeiro> financeiros, List<Custodia> custodias) {
        if (carteiraId <= 0 || acaoId <= 0 || financeiros == null || custodias == null) {
            throw new IllegalArgumentException("Contexto e fontes completos são obrigatórios");
        }
        List<EventoQuantitativo> eventos = new ArrayList<>();
        Set<Long> estornos = new HashSet<>();
        for (Financeiro f : financeiros) {
            if (f == null || f.tipo() == null) throw new IllegalArgumentException("Financeiro incompleto");
            Natureza natureza = switch (f.tipo()) {
                case COMPRA -> Natureza.COMPRA;
                case VENDA -> Natureza.VENDA;
            };
            eventos.add(new EventoQuantitativo(f.identidade(), f.carteiraId(), f.acaoId(), f.mercado(),
                    natureza, f.quantidade(), PosicaoCronologicaQuantitativa.financeira(
                    f.dataOperacao(), f.ordemNoDia()), f.grupo(), null, false));
        }
        for (Custodia c : custodias) {
            if (c == null || c.natureza() == null || c.vinculo() == null
                    || !c.vinculo().original().equals(c.identidade())) {
                throw new IllegalArgumentException("Custódia exige vínculo completo da própria original");
            }
            Long estornoId = c.vinculo().estornoId();
            if (estornoId != null && !estornos.add(estornoId)) {
                throw new IllegalArgumentException("Identidade de estorno duplicada");
            }
            Natureza natureza = switch (c.natureza()) {
                case TRANSFERENCIA -> Natureza.TRANSFERENCIA;
                case ATRIBUICAO -> Natureza.ATRIBUICAO;
            };
            eventos.add(new EventoQuantitativo(c.identidade(), c.carteiraId(), c.acaoId(), c.mercado(),
                    natureza, c.quantidade(), PosicaoCronologicaQuantitativa.custodia(c.dataEfetiva(),
                    c.ancoraFinanceira(), c.fracao()), c.origem(), c.destino(), estornoId != null));
        }
        Set<Identidade> identidades = new HashSet<>();
        Set<PosicaoCronologicaQuantitativa> posicoes = new HashSet<>();
        Mercado mercado = null;
        for (EventoQuantitativo evento : eventos) {
            if (evento.carteiraId() != carteiraId || evento.acaoId() != acaoId
                    || (mercado != null && evento.mercado() != mercado)) {
                throw new IllegalArgumentException("Fontes pertencem a contextos incompatíveis");
            }
            mercado = evento.mercado();
            if (!identidades.add(evento.identidade())) throw new IllegalArgumentException("Identidade duplicada");
            if (!posicoes.add(evento.posicao())) throw new IllegalArgumentException("Colisão estrutural de posição");
        }
        eventos.sort(java.util.Comparator.comparing(EventoQuantitativo::posicao));
        return List.copyOf(eventos);
    }
}
