package com.projeto.mappers;

import com.projeto.entities.*;
import com.projeto.services.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AtualizacaoCotacoesCarteiraMapperTest {
    @ParameterizedTest @ValueSource(strings={"REUSO","COOLDOWN_SUCESSO"})
    void atalhosPreservamSnapshotSemRecalcularLegado(String motivo) {
        var instante=OffsetDateTime.parse("2026-10-04T16:00:00Z");
        var estado=new AplicacaoCotacaoService.EstadoCotacao(new BigDecimal("102.000000"),
                ReferenciaTipoCotacao.DATA,null,LocalDate.of(2026,10,2),instante.minusMinutes(1),instante.plusDays(1));
        var item=new ResultadoAtualizacaoCotacao(9L,"AAPL","Apple",Mercado.EUA,Moeda.USD,
                RegraDecisaoCotacao.Resultado.SEM_NOVA_COTACAO,motivo,
                motivo.equals("REUSO")?ResultadoAtualizacaoCotacao.Origem.REUSO:ResultadoAtualizacaoCotacao.Origem.COOLDOWN,
                estado,null);
        var lote=new AtualizacaoCotacoesCarteiraService.Lote(1L,
                new AtualizacaoCotacoesCarteiraService.Resumo(0,1,0),List.of(item));
        var response=new AtualizacaoCotacoesCarteiraMapper().toResponse(lote);
        var cotacao=response.resultados().get(0).cotacao();
        assertSame(estado.preco(),cotacao.cotacaoAtual());
        assertSame(estado.data(),cotacao.referenciaMercadoCotacao().data());
        assertSame(estado.obtencao(),cotacao.cotacaoObtidaEm());
        assertSame(estado.legado(),cotacao.dataHoraCotacao());
        assertEquals(motivo,response.resultados().get(0).motivo());
        assertNull(response.resultados().get(0).erro());
        assertThrows(UnsupportedOperationException.class,()->response.resultados().clear());
    }
}
