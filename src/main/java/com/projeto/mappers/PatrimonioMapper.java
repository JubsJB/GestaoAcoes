package com.projeto.mappers;

import com.projeto.dto.PatrimonioMoedaResponse;
import com.projeto.dto.PatrimonioResponse;
import com.projeto.entities.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PatrimonioMapper {

    public PatrimonioMoedaResponse toMoedaResponse(Moeda moeda, BigDecimal patrimonioAtual) {
        return new PatrimonioMoedaResponse(moeda, patrimonioAtual);
    }

    public PatrimonioResponse toResponse(Long carteiraId, List<PatrimonioMoedaResponse> patrimonios) {
        return new PatrimonioResponse(carteiraId, List.copyOf(patrimonios));
    }
}
