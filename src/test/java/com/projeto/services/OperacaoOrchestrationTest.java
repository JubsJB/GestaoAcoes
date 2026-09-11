package com.projeto.services;
import com.projeto.dto.*;
import com.projeto.entities.*;
import com.projeto.integrations.cotacao.*;
import com.projeto.mappers.OperacaoMapper;
import com.projeto.repositories.*;
import com.projeto.services.exceptions.ApiException;
import com.projeto.validation.TickerNormalizer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperacaoOrchestrationTest {
 @Mock OperacaoRepository operacoes; @Mock CarteiraRepository carteiras; @Mock AcaoRepository acoes;
 @Mock CorretoraRepository corretoras; @Mock OperacaoPersistenceService persistence;
 @Mock CotacaoHistoricaProvider brasil; @Mock CotacaoHistoricaProvider eua;
 private OperacaoService service;
 @BeforeEach void setup(){
  lenient().when(brasil.mercado()).thenReturn(Mercado.BRASIL);lenient().when(eua.mercado()).thenReturn(Mercado.EUA);
  service=new OperacaoService(operacoes,carteiras,acoes,corretoras,new TickerNormalizer(),new OperacaoMapper(),
   Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"),ZoneOffset.UTC),persistence);
  clearInvocations(brasil,eua);
  lenient().when(carteiras.findById(1L)).thenReturn(Optional.of(mock(Carteira.class)));
  lenient().when(acoes.findByTickerAndMercado("PETR4",Mercado.BRASIL)).thenReturn(Optional.of(mock(Acao.class)));
  lenient().when(acoes.findByTickerAndMercado("AAPL",Mercado.EUA)).thenReturn(Optional.of(mock(Acao.class)));
 }
 @Test void purchaseUsesManualPriceWithoutProvider(){
  LocalDate date=LocalDate.of(2026,8,20);
  service.cadastrar(new OperacaoCompraCreateRequest(1L," petr4 ",Mercado.BRASIL,null,BigDecimal.TEN,date,new BigDecimal("32.47")));
  verifyNoInteractions(brasil,eua);
  ArgumentCaptor<OperacaoPersistenceCommand> command=ArgumentCaptor.forClass(OperacaoPersistenceCommand.class);
  verify(persistence).persistir(command.capture());assertEquals(new BigDecimal("32.470000"),command.getValue().precoUnitario());
 }
 @Test void saleNeverCallsProviderAndKeepsClientPrice(){
  service.cadastrar(new OperacaoVendaCreateRequest(1L,"AAPL",Mercado.EUA,null,BigDecimal.ONE,LocalDate.of(2026,8,20),new BigDecimal("20.25")));
  verifyNoInteractions(brasil,eua);
  ArgumentCaptor<OperacaoPersistenceCommand> command=ArgumentCaptor.forClass(OperacaoPersistenceCommand.class);
  verify(persistence).persistir(command.capture());assertEquals(new BigDecimal("20.250000"),command.getValue().precoUnitario());
 }
 @Test void invalidReferencesFailBeforeProviderAndPersistence(){
  when(carteiras.findById(404L)).thenReturn(Optional.empty());
  assertThrows(RuntimeException.class,()->service.cadastrar(new OperacaoCompraCreateRequest(404L,"PETR4",Mercado.BRASIL,null,BigDecimal.ONE,LocalDate.of(2026,8,20),BigDecimal.TEN)));
  verifyNoInteractions(brasil,eua,persistence);
 }
 @Test void missingZeroNegativeAndExcessPrecisionPricesFail(){
  for(BigDecimal price:Arrays.asList(null,BigDecimal.ZERO,BigDecimal.ONE.negate(),new BigDecimal("1.0000001"))){
   assertThrows(ApiException.class,()->service.cadastrar(new OperacaoCompraCreateRequest(1L,"PETR4",Mercado.BRASIL,null,BigDecimal.ONE,LocalDate.of(2026,8,20),price)));
  }
  verifyNoInteractions(brasil,eua,persistence);
 }
 @Test void unavailableProviderDoesNotBlockManualPurchase(){
  lenient().when(brasil.consultarFechamento(anyString(),any())).thenThrow(new IllegalStateException("unavailable"));
  service.cadastrar(new OperacaoCompraCreateRequest(1L,"PETR4",Mercado.BRASIL,null,BigDecimal.ONE,LocalDate.of(2026,8,20),BigDecimal.TEN));
  verifyNoInteractions(brasil,eua);verify(persistence).persistir(any());
 }
}
