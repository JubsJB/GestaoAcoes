package com.projeto.mappers;

import com.projeto.dto.PosicaoResponse;
import com.projeto.entities.Acao;
import com.projeto.services.CalculadoraPosicao.PosicaoCalculada;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PosicaoMapper {

    public PosicaoResponse toResponse(
            Acao acao,
            PosicaoCalculada posicao,
            BigDecimal valorAtualPosicao,
            BigDecimal resultadoNaoRealizado,
            BigDecimal rentabilidadePercentual
    ) {
        return new PosicaoResponse(
                acao.getId(),
                acao.getTicker(),
                acao.getNomeEmpresa(),
                acao.getMercado(),
                acao.getMoeda(),
                posicao.quantidadeAtual(),
                posicao.precoMedio(),
                posicao.custoPosicao(),
                acao.getCotacaoAtual(),
                acao.getDataHoraCotacao(),
                valorAtualPosicao,
                resultadoNaoRealizado,
                rentabilidadePercentual
        );
    }
}
