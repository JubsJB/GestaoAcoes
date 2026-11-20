package com.projeto.services;

import com.projeto.entities.PosicaoRacionalCustodia;
import java.time.LocalDate;

/** Ordem efetiva, independente da identidade e da ordem de registro.
 * A fração pertence ao núcleo de simulação; persistência exige paraPersistivel() explícito. */
public record PosicaoCronologicaQuantitativa(LocalDate dataEfetiva, int ancoraFinanceira,
        PosicaoRacionalTransitoria fracao) implements Comparable<PosicaoCronologicaQuantitativa> {
    public PosicaoCronologicaQuantitativa {
        if (dataEfetiva == null || ancoraFinanceira < 0 || (fracao == null && ancoraFinanceira == 0)) {
            throw new IllegalArgumentException("Data e âncora válidas são obrigatórias");
        }
        if (fracao != null) fracao.exigirEfetiva();
    }

    public static PosicaoCronologicaQuantitativa financeira(LocalDate data, int ordemNoDia) {
        return new PosicaoCronologicaQuantitativa(data, ordemNoDia, null);
    }

    public static PosicaoCronologicaQuantitativa custodia(LocalDate data, int ancora,
            PosicaoRacionalCustodia fracao) {
        if (fracao == null) throw new IllegalArgumentException("Custódia exige fração");
        return custodiaTransitoria(data, ancora, PosicaoRacionalTransitoria.dePersistivel(fracao));
    }

    public static PosicaoCronologicaQuantitativa custodiaTransitoria(LocalDate data, int ancora,
            PosicaoRacionalTransitoria fracao) {
        if (fracao == null) throw new IllegalArgumentException("Custódia exige fração transitória");
        return new PosicaoCronologicaQuantitativa(data, ancora, fracao);
    }

    public boolean financeira() { return fracao == null; }

    @Override
    public int compareTo(PosicaoCronologicaQuantitativa outra) {
        int ordem = dataEfetiva.compareTo(outra.dataEfetiva);
        if (ordem == 0) ordem = Integer.compare(ancoraFinanceira, outra.ancoraFinanceira);
        if (ordem == 0) {
            if (financeira()) return outra.financeira() ? 0 : -1;
            if (outra.financeira()) return 1;
            return fracao.compareTo(outra.fracao);
        }
        return ordem;
    }
}
