package com.projeto;
import com.projeto.dto.*;
import com.projeto.entities.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public final class TestOperacaoRequests {
 private TestOperacaoRequests(){}
 public static OperacaoCreateRequest request(Long carteiraId,String ticker,Mercado mercado,Long corretoraId,
  TipoOperacao tipo,BigDecimal quantidade,BigDecimal preco,LocalDate data){
  return tipo==TipoOperacao.COMPRA
   ?new OperacaoCompraCreateRequest(carteiraId,ticker,mercado,corretoraId,quantidade,data)
   :new OperacaoVendaCreateRequest(carteiraId,ticker,mercado,corretoraId,quantidade,data,preco);
 }
}
