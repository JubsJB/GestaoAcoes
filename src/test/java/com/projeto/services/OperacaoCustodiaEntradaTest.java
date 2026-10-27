package com.projeto.services;

import com.projeto.dto.*;
import com.projeto.entities.*;
import com.projeto.repositories.*;
import com.projeto.services.exceptions.ApiException;
import com.projeto.validation.TickerNormalizer;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperacaoCustodiaEntradaTest {
    final CarteiraRepository carteiras = mock(CarteiraRepository.class);
    final AcaoRepository acoes = mock(AcaoRepository.class);
    final CorretoraRepository corretoras = mock(CorretoraRepository.class);
    final OperacaoRepository operacoes = mock(OperacaoRepository.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);
    @ParameterizedTest @ValueSource(strings={"zero","carteira","corretora","preco","data","tipo","mercado","ausente","subtipo"})
    void compraInvalidaPrecedeReferenciaAusenteSemConsultarBanco(String caso) {
        var service = new OperacaoService(operacoes, carteiras, acoes, corretoras,
                new TickerNormalizer(), null, clock, null);
        OperacaoCreateRequest request = new OperacaoCompraCreateRequest(caso.equals("carteira")?0L:1L, "PETR4",
                caso.equals("mercado")?null:Mercado.BRASIL, caso.equals("corretora")?-1L:null,
                caso.equals("zero")?BigDecimal.ZERO:BigDecimal.ONE,
                caso.equals("data")?null:LocalDate.of(2026,10,1), caso.equals("preco")?BigDecimal.ZERO:BigDecimal.TEN);
        if(caso.equals("tipo"))request.setTipo(null);
        if(caso.equals("subtipo")) {
            request = new OperacaoVendaCreateRequest(1L,"PETR4",Mercado.BRASIL,null,BigDecimal.ONE,LocalDate.of(2026,10,1),BigDecimal.TEN);
            request.setTipo(TipoOperacao.COMPRA);
        }
        OperacaoCreateRequest entrada = caso.equals("ausente") ? null : request;
        assertEquals("REQUEST_INVALIDO", assertThrows(ApiException.class, () -> service.cadastrar(entrada)).getCode());
        verifyNoInteractions(operacoes, carteiras, acoes, corretoras);
    }
}
