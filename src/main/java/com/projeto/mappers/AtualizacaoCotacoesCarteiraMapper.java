package com.projeto.mappers;

import com.projeto.dto.*;
import com.projeto.services.AtualizacaoCotacoesCarteiraService.Lote;
import com.projeto.services.ResultadoAtualizacaoCotacao;
import org.springframework.stereotype.Component;

/** Adaptação pura dos snapshots: não relê estado e não recalcula metadados. */
@Component
public class AtualizacaoCotacoesCarteiraMapper {
    public AtualizacaoCotacoesCarteiraResponse toResponse(Lote lote) {
        var resumo = lote.resumo();
        return new AtualizacaoCotacoesCarteiraResponse(lote.carteiraId(),
                new ResumoAtualizacaoCotacoesResponse(resumo.atualizadas(), resumo.semNovaCotacao(), resumo.falhas()),
                lote.resultados().stream().map(this::toResponse).toList());
    }

    private ResultadoAtualizacaoCotacaoResponse toResponse(ResultadoAtualizacaoCotacao item) {
        var estado = item.cotacao();
        var erro = item.falha() == null ? null
                : new ErroAtualizacaoCotacaoResponse(item.falha().getCode(), item.falha().getMessage());
        var resultado = switch (item.resultado()) {
            case ATUALIZADA -> ResultadoAtualizacaoCotacaoResponse.Resultado.ATUALIZADA;
            case SEM_NOVA_COTACAO -> ResultadoAtualizacaoCotacaoResponse.Resultado.SEM_NOVA_COTACAO;
            case FALHA -> ResultadoAtualizacaoCotacaoResponse.Resultado.FALHA;
        };
        return new ResultadoAtualizacaoCotacaoResponse(item.acaoId(), item.ticker(), item.mercado(),
                resultado, item.motivo(), new EstadoCotacaoResponse(estado.preco(),
                new ReferenciaMercadoCotacaoResponse(estado.tipo(), estado.instante(), estado.data()),
                estado.obtencao(), estado.legado()), erro);
    }
}
