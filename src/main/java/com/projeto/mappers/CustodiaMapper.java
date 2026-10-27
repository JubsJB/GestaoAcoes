package com.projeto.mappers;

import com.projeto.dto.*;
import com.projeto.entities.*;
import org.springframework.stereotype.Component;
import static com.projeto.dto.RegistroCustodiaResponse.*;

@Component
public class CustodiaMapper {
    public RegistroCustodiaResponse original(MovimentacaoCustodia m, VinculoEstornoConsultado vinculo) {
        m.validarEstruturaHistorica();
        var situacao=m.getSituacao(vinculo);
        var e=vinculo.getEstorno();
        return new RegistroCustodiaResponse(m.getId(),TipoRegistro.MOVIMENTACAO,
                Natureza.valueOf(m.getNatureza().name()),situacao,m.getCarteiraId(),m.getAcaoId(),
                m.getAcao().getTicker(),m.getMercado(),m.getCorretoraOrigemId(),m.getCorretoraDestinoId(),
                m.getQuantidade(),m.getDataEfetiva(),m.getDataRegistro(),posicao(m),null,
                e==null?null:e.getId(),null);
    }
    public RegistroCustodiaResponse estorno(EstornoMovimentacaoCustodia e) {
        e.validarEstruturaHistorica();
        var m=e.getMovimentacaoOriginal();
        return new RegistroCustodiaResponse(e.getId(),TipoRegistro.ESTORNO,Natureza.ESTORNO,null,
                m.getCarteiraId(),m.getAcaoId(),m.getAcao().getTicker(),m.getMercado(),
                m.getCorretoraOrigemId(),m.getCorretoraDestinoId(),m.getQuantidade(),m.getDataEfetiva(),
                e.getDataRegistro(),posicao(m),m.getId(),e.getId(),e.getMotivo());
    }
    private PosicaoCustodiaResponse posicao(MovimentacaoCustodia m) {
        var p=m.getPosicaoRacional();
        return new PosicaoCustodiaResponse(m.getOrdemFinanceiraAncora(),p.getNumeradorTexto(),p.getDenominadorTexto());
    }
}
