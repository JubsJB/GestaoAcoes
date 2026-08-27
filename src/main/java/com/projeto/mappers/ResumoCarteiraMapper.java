package com.projeto.mappers;

import com.projeto.dto.ResumoCarteiraResponse;
import com.projeto.dto.ResumoMoedaResponse;
import com.projeto.services.AgregadorPosicoesPorMoeda.TotaisPorMoeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ResumoCarteiraMapper {

    public ResumoMoedaResponse toMoedaResponse(
            TotaisPorMoeda totais,
            BigDecimal rentabilidadePercentual
    ) {
        return new ResumoMoedaResponse(
                totais.moeda(),
                totais.custoTotalPosicoes(),
                totais.patrimonioAtual(),
                totais.resultadoNaoRealizadoTotal(),
                rentabilidadePercentual
        );
    }

    public ResumoCarteiraResponse toResponse(Long carteiraId, List<ResumoMoedaResponse> resumos) {
        return new ResumoCarteiraResponse(carteiraId, List.copyOf(resumos));
    }
}
