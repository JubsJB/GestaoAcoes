package com.projeto.services;

import com.projeto.entities.*;
import com.projeto.repositories.*;
import com.projeto.services.exceptions.*;
import com.projeto.validation.TickerNormalizer;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransferenciaCustodiaServiceTest {
    final CarteiraRepository carteiras=mock(CarteiraRepository.class);
    final AcaoRepository acoes=mock(AcaoRepository.class);
    final CorretoraRepository corretoras=mock(CorretoraRepository.class);
    final OperacaoRepository operacoes=mock(OperacaoRepository.class);
    final MovimentacaoCustodiaRepository movimentos=mock(MovimentacaoCustodiaRepository.class);
    final EstornoMovimentacaoCustodiaRepository estornos=mock(EstornoMovimentacaoCustodiaRepository.class);
    final Clock clock=Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"),ZoneOffset.UTC);
    TransferenciaCustodiaPersistenceService writer() {
        return new TransferenciaCustodiaPersistenceService(carteiras,acoes,corretoras,operacoes,movimentos,estornos,clock,new TickerNormalizer());
    }
    TransferenciaCustodiaService fachada(TransferenciaCustodiaPersistenceService writer) {
        return new TransferenciaCustodiaService(writer,new TickerNormalizer(),clock);
    }
    @Test void entradaAusenteFalhaAntesDeQualquerConsulta() {
        var service=fachada(writer());
        var erro=assertThrows(ApiException.class,()->service.transferir(null));
        assertEquals(ErrorCodes.REQUEST_INVALIDO,erro.getCode());
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);
    }
    @ParameterizedTest @ValueSource(strings={"carteira","ticker","mercado","origem","destino","iguais","quantidade","zero","negativa","casas","precisao","fracaoBR","data"})
    void estruturaInvalidaEhRejeitadaAntesDeReferencias(String caso) {
        var command=new TransferenciaCustodiaCommand(caso.equals("carteira")?0L:1L,caso.equals("ticker")?" ":"PETR4",
                caso.equals("mercado")?null:Mercado.BRASIL,caso.equals("origem")?null:10L,
                caso.equals("destino")?null:caso.equals("iguais")?10L:20L,
                switch(caso){case "quantidade"->null;case "zero"->BigDecimal.ZERO;case "negativa"->BigDecimal.ONE.negate();
                    case "casas"->new BigDecimal("1.0000001");case "precisao"->new BigDecimal("10000000000000");
                    case "fracaoBR"->new BigDecimal("0.1");default->new BigDecimal("40");},
                caso.equals("data")?null:LocalDate.of(2026,10,1));
        var erro=assertThrows(ApiException.class,()->fachada(writer()).transferir(command));
        assertEquals(ErrorCodes.REQUEST_INVALIDO,erro.getCode());
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);
    }
    @ParameterizedTest @CsvSource({"BRASIL,2026-10-24T02:30:00Z","EUA,2026-10-24T03:30:00Z"})
    void dataFuturaNaZonaDoMercadoEhRecusadaAntesDoBanco(Mercado mercado,String instante) {
        var agora=Clock.fixed(Instant.parse(instante),ZoneOffset.UTC);
        var service=new TransferenciaCustodiaService(writer(),new TickerNormalizer(),agora);
        var command=new TransferenciaCustodiaCommand(1L,"PETR4",mercado,10L,20L,BigDecimal.ONE,LocalDate.of(2026,10,24));
        var erro=assertThrows(ApiException.class,()->service.transferir(command));
        assertEquals("DATA_EFETIVA_FUTURA",erro.getCode());
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);
    }
}
