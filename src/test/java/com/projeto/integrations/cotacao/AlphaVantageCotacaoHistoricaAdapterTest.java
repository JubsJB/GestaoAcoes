package com.projeto.integrations.cotacao;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.projeto.services.exceptions.*;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import java.net.http.HttpTimeoutException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class AlphaVantageCotacaoHistoricaAdapterTest {
 private final ObjectMapper json=new ObjectMapper();
 private final AlphaVantageCotacaoHistoricaAdapter adapter=new AlphaVantageCotacaoHistoricaAdapter(RestClient.builder().baseUrl("http://localhost").build(),"key");
 @Test void callsOnlyDailyCompactWithConfiguredKey(){
  RestClient.Builder builder=RestClient.builder().baseUrl("http://alpha.test");
  MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
  AlphaVantageCotacaoHistoricaAdapter http=new AlphaVantageCotacaoHistoricaAdapter(builder.build(),"secret-placeholder");
  server.expect(requestTo("http://alpha.test/query?function=TIME_SERIES_DAILY&symbol=AAPL&outputsize=compact&apikey=secret-placeholder"))
   .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("{\"Time Series (Daily)\":{\"2026-08-20\":{\"4. close\":\"123.45\"}}}",MediaType.APPLICATION_JSON));
  assertEquals(new BigDecimal("123.45"),http.consultarFechamento("AAPL",LocalDate.of(2026,8,20)).close());server.verify();
 }
 @Test void returnsExactRawClose()throws Exception{
  var value=adapter.parse(node("{\"Time Series (Daily)\":{\"2026-08-20\":{\"4. close\":\"123.456\"}}}"),"AAPL",LocalDate.of(2026,8,20));
  assertEquals(new BigDecimal("123.456"),value.close());
 }
 @Test void missingDateInsideRangeIsUnavailable()throws Exception{
  ApiException e=failure(series(2),LocalDate.of(2026,8,19));assertEquals(ErrorCodes.COTACAO_HISTORICA_INDISPONIVEL,e.getCode());
 }
 @Test void olderThanFullCompactWindowIsOutOfRange(){
  ApiException e=failure(series(100),LocalDate.of(2025,1,1));assertEquals(ErrorCodes.HISTORICO_COTACAO_FORA_DO_ALCANCE,e.getCode());
 }
 @Test void olderThanShortSeriesIsInvalid(){
  ApiException e=failure(series(99),LocalDate.of(2025,1,1));assertEquals(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,e.getCode());
 }
 @Test void rejectsInvalidKeysAndCloses()throws Exception{
  for(String body:new String[]{"{\"Time Series (Daily)\":{\"bad\":{\"4. close\":\"1\"}}}",
   "{\"Time Series (Daily)\":{\"2026-08-20\":{}}}","{\"Time Series (Daily)\":{\"2026-08-20\":{\"4. close\":\"x\"}}}",
   "{\"Time Series (Daily)\":{\"2026-08-20\":{\"4. close\":\"0\"}}}",
   "{\"Time Series (Daily)\":{\"2026-08-20\":{\"4. close\":\"-1\"}}}"})
   assertEquals(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,failure(node(body),LocalDate.of(2026,8,20)).getCode());
 }
 @Test void classifiesRateLimitInvalidSymbolAndUnknownMessages()throws Exception{
  assertEquals(ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO,failure(node("{\"Note\":\"API rate limit reached\"}"),LocalDate.now()).getCode());
  assertEquals(ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO,failure(node("{\"Information\":\"API call frequency exceeded\"}"),LocalDate.now()).getCode());
  assertEquals(ErrorCodes.TICKER_INEXISTENTE,failure(node("{\"Error Message\":\"Invalid symbol\"}"),LocalDate.now()).getCode());
  assertEquals(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,failure(node("{\"Information\":\"unknown\"}"),LocalDate.now()).getCode());
  assertEquals(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,failure(node("{\"Information\":\"This API call is invalid\"}"),LocalDate.now()).getCode());
  assertEquals(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,failure(node("{\"Error Message\":\"Invalid API call.\"}"),LocalDate.now()).getCode());
  assertEquals(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,failure(node("{\"Note\":\"Generic provider note\"}"),LocalDate.now()).getCode());
  assertEquals(ErrorCodes.TICKER_INEXISTENTE,failure(node("{\"Error Message\":\"The symbol AAPL is invalid\"}"),LocalDate.now()).getCode());
 }
 @Test void rejectsAbsentEmptyAndMalformedSeries()throws Exception{
  for(String body:new String[]{"{}","{\"Time Series (Daily)\":{}}","{\"Time Series (Daily)\":[]}"})
   assertEquals(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,failure(node(body),LocalDate.now()).getCode());
 }
 @Test void rejectsDuplicateDateKeysDuringRawJsonParsing(){
  String duplicated="{\"Time Series (Daily)\":{\"2026-08-20\":{\"4. close\":\"10\"},\"2026-08-20\":{\"4. close\":\"11\"}}}";
  assertHttpBody(duplicated,ErrorCodes.RESPOSTA_EXTERNA_INVALIDA);
 }
 @Test void classifiesHttpAndTransportFailuresWithoutNetwork(){
  assertHttpStatus(HttpStatus.TOO_MANY_REQUESTS,ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO);
  assertHttpStatus(HttpStatus.SERVICE_UNAVAILABLE,ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL);
  assertHttpStatus(HttpStatus.BAD_REQUEST,ErrorCodes.RESPOSTA_EXTERNA_INVALIDA);
  RestClient timeoutClient=RestClient.builder().requestFactory((uri,method)->{throw new HttpTimeoutException("timeout");}).build();
  assertCode(ErrorCodes.SERVICO_EXTERNO_TIMEOUT,()->new AlphaVantageCotacaoHistoricaAdapter(timeoutClient,"key").consultarFechamento("AAPL",LocalDate.of(2026,8,20)));
  RestClient unavailableClient=RestClient.builder().requestFactory((uri,method)->{throw new java.net.ConnectException("offline");}).build();
  assertCode(ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,()->new AlphaVantageCotacaoHistoricaAdapter(unavailableClient,"key").consultarFechamento("AAPL",LocalDate.of(2026,8,20)));
  assertCode(ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,()->new AlphaVantageCotacaoHistoricaAdapter(RestClient.create()," ").consultarFechamento("AAPL",LocalDate.of(2026,8,20)));
 }
 private void assertHttpBody(String body,String code){
  RestClient.Builder builder=RestClient.builder().baseUrl("http://alpha.test");MockRestServiceServer local=MockRestServiceServer.bindTo(builder).build();
  local.expect(requestTo("http://alpha.test/query?function=TIME_SERIES_DAILY&symbol=AAPL&outputsize=compact&apikey=key"))
   .andRespond(withSuccess(body,MediaType.APPLICATION_JSON));
  assertCode(code,()->new AlphaVantageCotacaoHistoricaAdapter(builder.build(),"key").consultarFechamento("AAPL",LocalDate.of(2026,8,20)));local.verify();
 }
 private void assertHttpStatus(HttpStatus status,String code){
  RestClient.Builder builder=RestClient.builder().baseUrl("http://alpha.test");MockRestServiceServer local=MockRestServiceServer.bindTo(builder).build();
  local.expect(requestTo("http://alpha.test/query?function=TIME_SERIES_DAILY&symbol=AAPL&outputsize=compact&apikey=key"))
   .andRespond(withStatus(status));
  assertCode(code,()->new AlphaVantageCotacaoHistoricaAdapter(builder.build(),"key").consultarFechamento("AAPL",LocalDate.of(2026,8,20)));local.verify();
 }
 private void assertCode(String code,Runnable call){ApiException e=assertThrows(ApiException.class,call::run);assertEquals(code,e.getCode());}
 private ObjectNode series(int count){ObjectNode root=json.createObjectNode(),s=root.putObject("Time Series (Daily)");
  LocalDate d=LocalDate.of(2026,8,20);for(int i=0;i<count;i++)s.putObject(d.minusDays(i*2L).toString()).put("4. close","10.25");return root;}
 private ApiException failure(JsonNode n,LocalDate d){return assertThrows(ApiException.class,()->adapter.parse(n,"AAPL",d));}
 private JsonNode node(String body)throws Exception{return json.readTree(body);}
}
