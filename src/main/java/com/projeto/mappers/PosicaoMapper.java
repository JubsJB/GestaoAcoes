package com.projeto.mappers;

import com.projeto.dto.PosicaoResponse;
import com.projeto.entities.Acao;
import com.projeto.services.CalculadoraPosicao.PosicaoCalculada;
import org.springframework.stereotype.Component;

@Component
public class PosicaoMapper {

    public PosicaoResponse toResponse(Acao acao, PosicaoCalculada posicao) {
        return new PosicaoResponse(
                acao.getId(),
                acao.getTicker(),
                acao.getNomeEmpresa(),
                acao.getMercado(),
                acao.getMoeda(),
                posicao.quantidadeAtual(),
                posicao.precoMedio(),
                posicao.custoPosicao()
        );
    }
}
