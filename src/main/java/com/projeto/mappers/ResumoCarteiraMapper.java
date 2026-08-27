package com.projeto.mappers;

import com.projeto.dto.ResumoCarteiraResponse;
import com.projeto.dto.ResumoMoedaResponse;
import com.projeto.services.AgregadorPosicoesPorMoeda.TotaisPorMoeda;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResumoCarteiraMapper {

    public ResumoMoedaResponse toMoedaResponse(TotaisPorMoeda totais) {
        return new ResumoMoedaResponse(
                totais.moeda(),
                totais.custoTotalPosicoes(),
                totais.patrimonioAtual(),
                totais.resultadoNaoRealizadoTotal()
        );
    }

    public ResumoCarteiraResponse toResponse(Long carteiraId, List<ResumoMoedaResponse> resumos) {
        return new ResumoCarteiraResponse(carteiraId, List.copyOf(resumos));
    }
}
