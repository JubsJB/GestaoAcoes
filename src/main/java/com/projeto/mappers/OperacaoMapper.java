package com.projeto.mappers;

import com.projeto.dto.OperacaoResponse;
import com.projeto.entities.Operacao;
import org.springframework.stereotype.Component;

@Component
public class OperacaoMapper {

    public OperacaoResponse toResponse(Operacao operacao) {
        return new OperacaoResponse(
                operacao.getId(),
                operacao.getCarteira().getId(),
                operacao.getAcao().getTicker(),
                operacao.getAcao().getMercado(),
                operacao.getCorretora() == null ? null : operacao.getCorretora().getId(),
                operacao.getTipo(),
                operacao.getQuantidade(),
                operacao.getPrecoUnitario(),
                operacao.getDataOperacao(),
                operacao.getOrdemNoDia(),
                operacao.getValorTotal()
        );
    }
}
