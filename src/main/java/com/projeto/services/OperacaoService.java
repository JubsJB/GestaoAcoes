package com.projeto.services;
import com.projeto.dto.*;
import com.projeto.entities.*;
import com.projeto.mappers.OperacaoMapper;
import com.projeto.repositories.*;
import com.projeto.services.exceptions.*;
import com.projeto.validation.TickerNormalizer;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class OperacaoService {
 private static final int PRECISION=19,SCALE=6;
 private static final ZoneId BRAZIL=ZoneId.of("America/Sao_Paulo"),USA=ZoneId.of("America/New_York");
 private static final Sort QUERY_ORDER=Sort.by(Sort.Order.asc("dataOperacao"),Sort.Order.asc("ordemNoDia"),Sort.Order.asc("id"));
 private final OperacaoRepository operacoes; private final CarteiraRepository carteiras; private final AcaoRepository acoes;
 private final CorretoraRepository corretoras; private final TickerNormalizer tickers; private final OperacaoMapper mapper;
 private final Clock clock; private final OperacaoPersistenceService persistence;
 public OperacaoService(OperacaoRepository operacoes,CarteiraRepository carteiras,AcaoRepository acoes,
  CorretoraRepository corretoras,TickerNormalizer tickers,OperacaoMapper mapper,Clock clock,
  OperacaoPersistenceService persistence){
  this.operacoes=operacoes;this.carteiras=carteiras;this.acoes=acoes;this.corretoras=corretoras;this.tickers=tickers;
  this.mapper=mapper;this.clock=clock;this.persistence=persistence;
 }
 public OperacaoResponse cadastrar(OperacaoCreateRequest request){
  if(request==null)throw OperacaoPersistenceCommand.invalida("request","Corpo da requisição é obrigatório");
  BigDecimal price;
  if(request instanceof OperacaoVendaCreateRequest sale && request.getTipo()==TipoOperacao.VENDA)price=sale.getPrecoUnitario();
  else if(request instanceof OperacaoCompraCreateRequest purchase && request.getTipo()==TipoOperacao.COMPRA)price=purchase.getPrecoUnitario();
  else throw OperacaoPersistenceCommand.invalida("tipo","Tipo de operação inválido");
  var command = new OperacaoPersistenceCommand(request.getCarteiraId(),request.getTicker(),request.getMercado(),
          request.getCorretoraId(),request.getTipo(),request.getQuantidade(),price,request.getDataOperacao()).validada(clock,tickers);
  try { return persistence.persistir(command); }
  catch (RuntimeException falha) { throw FalhasEscritaOperacao.traduzir(falha); }
 }
 @Transactional(readOnly=true) public List<OperacaoResponse> listar(){return operacoes.findAll(QUERY_ORDER).stream().map(mapper::toResponse).toList();}
 @Transactional(readOnly=true) public OperacaoResponse buscarPorId(Long id){return mapper.toResponse(operacoes.findById(id).orElseThrow(()->new ObjectNotFoundException("Operação não encontrada para o id: "+id)));}
 @Transactional(readOnly=true) public List<OperacaoResponse> listarPorCarteira(Long id){carteiras.findById(id).orElseThrow(()->new ObjectNotFoundException("Carteira não encontrada para o id: "+id));return operacoes.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(id).stream().map(mapper::toResponse).toList();}
}
