package com.projeto.services;

import com.projeto.entities.*;
import com.projeto.services.exceptions.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.projeto.services.RegraDecisaoCotacao.Resultado.*;
import static com.projeto.services.ResultadoAtualizacaoCotacao.Origem.*;
import static org.junit.jupiter.api.Assertions.*;

class ResultadoAtualizacaoCotacaoTest {
    final AplicacaoCotacaoService.EstadoCotacao estado = new AplicacaoCotacaoService.EstadoCotacao(
            new BigDecimal("100.123456"), ReferenciaTipoCotacao.DATA,null,java.time.LocalDate.of(2026,10,2),
            OffsetDateTime.parse("2026-10-04T16:00:00Z"),
            OffsetDateTime.parse("2026-10-04T16:00:00Z"));
    final ApiException erro = new ApiException(HttpStatus.GATEWAY_TIMEOUT,"SERVICO_EXTERNO_TIMEOUT","Timeout");

    ResultadoAtualizacaoCotacao resultado(RegraDecisaoCotacao.Resultado r,
            ResultadoAtualizacaoCotacao.Origem origem,String motivo,ApiException falha) {
        return new ResultadoAtualizacaoCotacao(1L,"AAA","Empresa",Mercado.EUA,Moeda.USD,
                r,motivo,origem,estado,falha);
    }

    @Test void combinacoesValidasPreservamSnapshotLossless() {
        var validos=List.of(
                resultado(ATUALIZADA,CONSULTA,"REFERENCIA_INICIAL_ESTABELECIDA",null),
                resultado(ATUALIZADA,CONSULTA,"REFERENCIA_POSTERIOR",null),
                resultado(SEM_NOVA_COTACAO,CONSULTA,"MESMA_OBSERVACAO",null),
                resultado(SEM_NOVA_COTACAO,CONSULTA,"REFERENCIA_ANTERIOR",null),
                resultado(SEM_NOVA_COTACAO,REUSO,"REUSO",null),
                resultado(SEM_NOVA_COTACAO,COOLDOWN,"COOLDOWN_SUCESSO",null),
                resultado(FALHA,CONSULTA,"SERVICO_EXTERNO_TIMEOUT",erro),
                resultado(FALHA,COOLDOWN,"COOLDOWN_FALHA",erro));
        validos.forEach(r->assertSame(estado,r.cotacao()));
        assertSame(estado,validos.get(0).emCooldown().cotacao());
        assertEquals(new BigDecimal("100.123456"),validos.get(0).cotacao().preco());
    }

    @ParameterizedTest @EnumSource(ResultadoAtualizacaoCotacao.Origem.class)
    void somenteFalhaCarregaErro(ResultadoAtualizacaoCotacao.Origem origem) {
        for(var r:RegraDecisaoCotacao.Resultado.values()) {
            String motivo=origem==COOLDOWN?(r==FALHA?"COOLDOWN_FALHA":"COOLDOWN_SUCESSO"):
                    origem==REUSO?"REUSO":r==ATUALIZADA?"REFERENCIA_POSTERIOR":"MESMA_OBSERVACAO";
            assertThrows(IllegalArgumentException.class,()->resultado(r,origem,motivo,r==FALHA?null:erro));
        }
    }

    @Test void classificacaoOrigemMotivoIncompativeisSaoRejeitados() {
        assertThrows(IllegalArgumentException.class,()->resultado(ATUALIZADA,REUSO,"REUSO",null));
        assertThrows(IllegalArgumentException.class,()->resultado(ATUALIZADA,COOLDOWN,"COOLDOWN_SUCESSO",null));
        assertThrows(IllegalArgumentException.class,()->resultado(FALHA,REUSO,"REUSO",erro));
        assertThrows(IllegalArgumentException.class,()->resultado(ATUALIZADA,CONSULTA,"MESMA_OBSERVACAO",null));
        assertThrows(IllegalArgumentException.class,()->resultado(SEM_NOVA_COTACAO,CONSULTA,"REFERENCIA_POSTERIOR",null));
        assertThrows(IllegalArgumentException.class,()->resultado(FALHA,COOLDOWN,"COOLDOWN_SUCESSO",erro));
        assertThrows(IllegalArgumentException.class,()->resultado(SEM_NOVA_COTACAO,COOLDOWN,"REUSO",null));
        assertThrows(IllegalArgumentException.class,()->resultado(FALHA,CONSULTA,"COOLDOWN_FALHA",erro));
        assertThrows(IllegalArgumentException.class,()->resultado(FALHA,CONSULTA,"SERVICO_EXTERNO_TIMEOUT",erro).emCooldown());
        assertThrows(IllegalArgumentException.class,()->resultado(SEM_NOVA_COTACAO,COOLDOWN,"COOLDOWN_SUCESSO",null).comSnapshot(estado));
    }

    @Test void camposObrigatoriosNaoPodemSerNulosOuVazios() {
        Object[] campos={1L,"AAA","Empresa",Mercado.EUA,Moeda.USD,SEM_NOVA_COTACAO,
                "REUSO",REUSO,estado};
        for(int i=0;i<campos.length;i++) {
            Object[] c=campos.clone();c[i]=null;
            assertThrows(IllegalArgumentException.class,()->new ResultadoAtualizacaoCotacao(
                    (Long)c[0],(String)c[1],(String)c[2],(Mercado)c[3],(Moeda)c[4],
                    (RegraDecisaoCotacao.Resultado)c[5],(String)c[6],
                    (ResultadoAtualizacaoCotacao.Origem)c[7],(AplicacaoCotacaoService.EstadoCotacao)c[8],null));
        }
        assertThrows(IllegalArgumentException.class,()->resultado(SEM_NOVA_COTACAO,REUSO," ",null));
    }

    @Test void resumoRejeitaNegativosELoteValidaCadaCategoriaETotal() {
        assertThrows(IllegalArgumentException.class,()->new AtualizacaoCotacoesCarteiraService.Resumo(-1,0,0));
        assertThrows(IllegalArgumentException.class,()->new AtualizacaoCotacoesCarteiraService.Resumo(0,-1,0));
        assertThrows(IllegalArgumentException.class,()->new AtualizacaoCotacoesCarteiraService.Resumo(0,0,-1));
        var r=resultado(SEM_NOVA_COTACAO,REUSO,"REUSO",null);
        assertThrows(IllegalArgumentException.class,()->new AtualizacaoCotacoesCarteiraService.Lote(1L,
                new AtualizacaoCotacoesCarteiraService.Resumo(1,0,0),List.of(r)));
        assertThrows(IllegalArgumentException.class,()->new AtualizacaoCotacoesCarteiraService.Lote(1L,
                new AtualizacaoCotacoesCarteiraService.Resumo(0,0,0),List.of(r)));
        assertThrows(IllegalArgumentException.class,()->new AtualizacaoCotacoesCarteiraService.Lote(1L,
                new AtualizacaoCotacoesCarteiraService.Resumo(Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE),List.of(r)));
    }

    @Test void loteRejeitaDuplicatasENulidadeEPreservaCopiaDefensiva() {
        var r=resultado(SEM_NOVA_COTACAO,REUSO,"REUSO",null);
        var resumo=new AtualizacaoCotacoesCarteiraService.Resumo(0,1,0);
        assertThrows(IllegalArgumentException.class,()->new AtualizacaoCotacoesCarteiraService.Lote(1L,
                new AtualizacaoCotacoesCarteiraService.Resumo(0,2,0),List.of(r,r)));
        assertThrows(IllegalArgumentException.class,()->new AtualizacaoCotacoesCarteiraService.Lote(null,resumo,List.of(r)));
        assertThrows(IllegalArgumentException.class,()->new AtualizacaoCotacoesCarteiraService.Lote(1L,null,List.of(r)));
        assertThrows(IllegalArgumentException.class,()->new AtualizacaoCotacoesCarteiraService.Lote(1L,resumo,null));
        var entrada=new ArrayList<>(List.of(r));
        var lote=new AtualizacaoCotacoesCarteiraService.Lote(1L,resumo,entrada);entrada.clear();
        assertEquals(1,lote.resultados().size());
        assertThrows(UnsupportedOperationException.class,()->lote.resultados().clear());
        assertTrue(new AtualizacaoCotacoesCarteiraService.Lote(1L,
                new AtualizacaoCotacoesCarteiraService.Resumo(0,0,0),List.of()).resultados().isEmpty());
    }
}
