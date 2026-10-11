package com.projeto.entities;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class AcaoObservacaoMonetariaTest {
    static final OffsetDateTime O = OffsetDateTime.parse("2026-10-03T15:05:00Z");
    Acao acao() { return new Acao("AAPL","Apple",Mercado.EUA,Moeda.USD,new BigDecimal("100"),O,
            ReferenciaTipoCotacao.DATA,null,LocalDate.of(2026,10,1),O); }
    java.util.List<Object> estado(Acao a) { return Arrays.asList(a.getCotacaoAtual(),a.getReferenciaTipo(),
            a.getReferenciaData(),a.getReferenciaInstante(),a.getCotacaoObtidaEm(),a.getDataHoraCotacao()); }
    @ParameterizedTest
    @ValueSource(strings={"1","1.2","1.123456","1.230000","1.1234560000","9999999999999.999999","0.000001"})
    void aceitaExatamenteSemArredondar(String valor) {
        var a=acao(); var preco=new BigDecimal(valor);
        a.substituirObservacao(preco,ReferenciaTipoCotacao.INSTANTE,O,null,O.minusMinutes(1));
        assertEquals(0,preco.compareTo(a.getCotacaoAtual())); assertEquals(6,a.getCotacaoAtual().scale());
        assertNull(a.getReferenciaData()); assertEquals(O,a.getReferenciaInstante());
    }
    @ParameterizedTest
    @ValueSource(strings={"1.1234567","0.0000001","10000000000000","9999999999999.9999991","12345678901234567890.123456","0","-1"})
    void rejeitaSemQualquerMutacao(String valor) {
        var a=acao(); var antes=estado(a);
        assertThrows(IllegalArgumentException.class,()->a.substituirObservacao(new BigDecimal(valor),
                ReferenciaTipoCotacao.INSTANTE,O,null,O.minusMinutes(1)));
        assertEquals(antes,estado(a));
    }
    @Test void rejeitaNuloSemMutacao() {
        var a=acao(); var antes=estado(a);
        assertThrows(IllegalArgumentException.class,()->a.substituirObservacao(null,ReferenciaTipoCotacao.DATA,
                null,LocalDate.of(2026,10,2),O)); assertEquals(antes,estado(a));
    }
}
