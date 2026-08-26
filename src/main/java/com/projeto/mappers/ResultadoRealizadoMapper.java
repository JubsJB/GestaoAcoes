package com.projeto.mappers;

import com.projeto.dto.ResultadoRealizadoResponse;
import com.projeto.entities.Acao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ResultadoRealizadoMapper {

    public ResultadoRealizadoResponse toResponse(Acao acao, BigDecimal resultadoRealizado) {
        return new ResultadoRealizadoResponse(
                acao.getId(),
                acao.getTicker(),
                acao.getNomeEmpresa(),
                acao.getMercado(),
                acao.getMoeda(),
                resultadoRealizado
        );
    }
}
