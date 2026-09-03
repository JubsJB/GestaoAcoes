package com.projeto.services;
import com.projeto.dto.*;
import com.projeto.entities.*;
import com.projeto.integrations.cotacao.*;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OperacaoService {
 private static final int PRECISION=19,SCALE=6;
 private static final ZoneId BRAZIL=ZoneId.of("America/Sao_Paulo"),USA=ZoneId.of("America/New_York");
 private static final Sort QUERY_ORDER=Sort.by(Sort.Order.asc("dataOperacao"),Sort.Order.asc("ordemNoDia"),Sort.Order.asc("id"));
 private final OperacaoRepository operacoes; private final CarteiraRepository carteiras; private final AcaoRepository acoes;
 private final CorretoraRepository corretoras; private final TickerNormalizer tickers; private final OperacaoMapper mapper;
 private final Clock clock; private final OperacaoPersistenceService persistence; private final Map<Mercado,CotacaoHistoricaProvider> providers;
 public OperacaoService(OperacaoRepository operacoes,CarteiraRepository carteiras,AcaoRepository acoes,
  CorretoraRepository corretoras,TickerNormalizer tickers,OperacaoMapper mapper,Clock clock,
  OperacaoPersistenceService persistence,List<CotacaoHistoricaProvider> providers){
  this.operacoes=operacoes;this.carteiras=carteiras;this.acoes=acoes;this.corretoras=corretoras;this.tickers=tickers;
  this.mapper=mapper;this.clock=clock;this.persistence=persistence;
  try{this.providers=providers.stream().collect(Collectors.toUnmodifiableMap(CotacaoHistoricaProvider::mercado,Function.identity()));}
  catch(IllegalStateException e){throw new IllegalStateException("Mais de um provider histórico para o mesmo mercado",e);}
 }
 public OperacaoResponse cadastrar(OperacaoCreateRequest request){
  if(request==null)throw invalid("request","Corpo da requisição é obrigatório");
  if(request.getCarteiraId()==null)throw invalid("carteiraId","Carteira é obrigatória");
  if(request.getMercado()==null)throw invalid("mercado","Mercado é obrigatório");
  if(request.getTipo()==null)throw invalid("tipo","Tipo é obrigatório");
  String ticker=tickers.normalizeAndValidate(request.getTicker());
  carteiras.findById(request.getCarteiraId()).orElseThrow(()->new ObjectNotFoundException("Carteira não encontrada para o id: "+request.getCarteiraId()));
  acoes.findByTickerAndMercado(ticker,request.getMercado()).orElseThrow(()->new ObjectNotFoundException("Ação não encontrada para ticker "+ticker+" no mercado "+request.getMercado()));
  if(request.getCorretoraId()!=null)corretoras.findById(request.getCorretoraId()).orElseThrow(()->new ObjectNotFoundException("Corretora não encontrada para o id: "+request.getCorretoraId()));
  BigDecimal quantity=operand(request.getQuantidade(),"quantidade","Quantidade");
  if(request.getMercado()==Mercado.BRASIL&&quantity.stripTrailingZeros().scale()>0)throw invalid("quantidade","Quantidade deve ser matematicamente inteira para o mercado BRASIL");
  validateDate(request.getDataOperacao(),request.getMercado());
  BigDecimal price;
  if(request instanceof OperacaoVendaCreateRequest sale)price=operand(sale.getPrecoUnitario(),"precoUnitario","Preço unitário");
  else if(request instanceof OperacaoCompraCreateRequest){
   CotacaoHistoricaProvider provider=providers.get(request.getMercado());
   if(provider==null)throw com.projeto.integrations.ExternalApiErrorMapper.unavailable("cotação histórica");
   CotacaoHistoricaData quote=provider.consultarFechamento(ticker,request.getDataOperacao());
   price=validateQuote(quote,ticker,request.getDataOperacao());
  }else throw invalid("tipo","Tipo de operação inválido");
  return persistence.persistir(new OperacaoPersistenceCommand(request.getCarteiraId(),ticker,request.getMercado(),request.getCorretoraId(),request.getTipo(),quantity,price,request.getDataOperacao()));
 }
 private BigDecimal validateQuote(CotacaoHistoricaData q,String ticker,LocalDate date){
  if(q==null||!ticker.equals(q.ticker())||!date.equals(q.dataPregao()))throw com.projeto.integrations.ExternalApiErrorMapper.invalidResponse("cotação histórica");
  try{return operand(q.close(),"precoUnitario","Fechamento histórico");}catch(ApiException e){throw com.projeto.integrations.ExternalApiErrorMapper.invalidResponse("cotação histórica");}
 }
 private BigDecimal operand(BigDecimal value,String field,String label){
  if(value==null)throw invalid(field,label+" é obrigatório");if(value.signum()<=0)throw invalid(field,label+" deve ser maior que zero");
  if(value.scale()>SCALE)throw invalid(field,label+" deve possuir no máximo 6 casas decimais");
  BigDecimal n;try{n=value.setScale(SCALE,RoundingMode.UNNECESSARY);}catch(ArithmeticException e){throw invalid(field,label+" não pode ser arredondado ou truncado");}
  if(n.precision()>PRECISION)throw invalid(field,label+" excede a precisão máxima 19");return n;
 }
 private void validateDate(LocalDate date,Mercado market){if(date==null)throw invalid("dataOperacao","Data da operação é obrigatória");
  if(date.isAfter(LocalDate.now(clock.withZone(market==Mercado.BRASIL?BRAZIL:USA))))throw invalid("dataOperacao","Data da operação não pode ser futura");}
 private ApiException invalid(String f,String m){return new ApiException(HttpStatus.BAD_REQUEST,ErrorCodes.REQUEST_INVALIDO,"Dados da requisição inválidos",Map.of(f,m));}
 @Transactional(readOnly=true) public List<OperacaoResponse> listar(){return operacoes.findAll(QUERY_ORDER).stream().map(mapper::toResponse).toList();}
 @Transactional(readOnly=true) public OperacaoResponse buscarPorId(Long id){return mapper.toResponse(operacoes.findById(id).orElseThrow(()->new ObjectNotFoundException("Operação não encontrada para o id: "+id)));}
 @Transactional(readOnly=true) public List<OperacaoResponse> listarPorCarteira(Long id){carteiras.findById(id).orElseThrow(()->new ObjectNotFoundException("Carteira não encontrada para o id: "+id));return operacoes.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(id).stream().map(mapper::toResponse).toList();}
}
