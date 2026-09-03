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
   Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"),ZoneOffset.UTC),persistence,List.of(brasil,eua));
  clearInvocations(brasil,eua);
  lenient().when(carteiras.findById(1L)).thenReturn(Optional.of(mock(Carteira.class)));
  lenient().when(acoes.findByTickerAndMercado("PETR4",Mercado.BRASIL)).thenReturn(Optional.of(mock(Acao.class)));
  lenient().when(acoes.findByTickerAndMercado("AAPL",Mercado.EUA)).thenReturn(Optional.of(mock(Acao.class)));
 }
 @Test void purchaseUsesOnlyProviderForItsMarketAndPassesReturnedPrice(){
  LocalDate date=LocalDate.of(2026,8,20);
  when(brasil.consultarFechamento("PETR4",date)).thenReturn(new CotacaoHistoricaData("PETR4",date,new BigDecimal("32.47")));
  service.cadastrar(new OperacaoCompraCreateRequest(1L," petr4 ",Mercado.BRASIL,null,BigDecimal.TEN,date));
  verify(brasil).consultarFechamento("PETR4",date);verifyNoInteractions(eua);
  ArgumentCaptor<OperacaoPersistenceCommand> command=ArgumentCaptor.forClass(OperacaoPersistenceCommand.class);
  verify(persistence).persistir(command.capture());assertEquals(new BigDecimal("32.470000"),command.getValue().precoUnitario());
 }
 @Test void saleNeverCallsProviderAndKeepsClientPrice(){
  LocalDate date=LocalDate.of(2026,8,20);
  service.cadastrar(new OperacaoVendaCreateRequest(1L,"AAPL",Mercado.EUA,null,BigDecimal.ONE,date,new BigDecimal("20.25")));
  verifyNoInteractions(brasil,eua);
  ArgumentCaptor<OperacaoPersistenceCommand> command=ArgumentCaptor.forClass(OperacaoPersistenceCommand.class);
  verify(persistence).persistir(command.capture());assertEquals(new BigDecimal("20.250000"),command.getValue().precoUnitario());
 }
 @Test void invalidReferencesFailBeforeProviderAndPersistence(){
  when(carteiras.findById(404L)).thenReturn(Optional.empty());
  assertThrows(RuntimeException.class,()->service.cadastrar(new OperacaoCompraCreateRequest(404L,"PETR4",Mercado.BRASIL,null,BigDecimal.ONE,LocalDate.of(2026,8,20))));
  verifyNoInteractions(brasil,eua,persistence);
 }
 @Test void missingProviderFailsBeforeTransactionalCollaborator(){
  OperacaoService withoutBrazil=new OperacaoService(operacoes,carteiras,acoes,corretoras,new TickerNormalizer(),new OperacaoMapper(),
   Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"),ZoneOffset.UTC),persistence,List.of(eua));
  ApiException error=assertThrows(ApiException.class,()->withoutBrazil.cadastrar(
   new OperacaoCompraCreateRequest(1L,"PETR4",Mercado.BRASIL,null,BigDecimal.ONE,LocalDate.of(2026,8,20))));
  assertEquals(com.projeto.services.exceptions.ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,error.getCode());
  verifyNoInteractions(persistence);
 }
 @Test void externalFailureEndsBeforeTransactionalPersistenceAndLock(){
  LocalDate date=LocalDate.of(2026,8,20);
  when(brasil.consultarFechamento("PETR4",date)).thenThrow(
   com.projeto.integrations.ExternalApiErrorMapper.invalidResponse("BRAPI"));
  assertThrows(ApiException.class,()->service.cadastrar(
   new OperacaoCompraCreateRequest(1L,"PETR4",Mercado.BRASIL,null,BigDecimal.ONE,date)));
  verifyNoInteractions(persistence);
  verify(carteiras,never()).findByIdForUpdate(anyLong());
 }
}
