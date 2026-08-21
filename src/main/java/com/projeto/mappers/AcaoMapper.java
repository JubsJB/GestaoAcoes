package com.projeto.mappers;

import com.projeto.dto.AcaoResponse;
import com.projeto.entities.Acao;
import org.springframework.stereotype.Component;

@Component
public class AcaoMapper {

    public AcaoResponse toResponse(Acao acao) {
        return new AcaoResponse(
                acao.getId(),
                acao.getTicker(),
                acao.getNomeEmpresa(),
                acao.getMercado(),
                acao.getMoeda(),
                acao.getCotacaoAtual(),
                acao.getDataHoraCotacao()
        );
    }
}
