package com.projeto.integrations.cotacao;
import com.fasterxml.jackson.databind.*;
import com.projeto.services.exceptions.*;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import java.net.http.HttpTimeoutException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class BrapiCotacaoHistoricaAdapterTest {
 private final ObjectMapper json=new ObjectMapper();
 private final BrapiCotacaoHistoricaAdapter adapter=new BrapiCotacaoHistoricaAdapter(RestClient.builder().baseUrl("http://localhost").build(),"key");
 private final LocalDate date=LocalDate.of(2026,8,20);
 @Test void callsApprovedEndpointParametersAndBearer(){
  RestClient.Builder builder=RestClient.builder().baseUrl("http://brapi.test");
  MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
  BrapiCotacaoHistoricaAdapter http=new BrapiCotacaoHistoricaAdapter(builder.build(),"secret-placeholder");
  server.expect(requestTo("http://brapi.test/api/v2/stocks/historical?symbols=PETR4&startDate=2026-08-20&endDate=2026-08-20&interval=1d"))
   .andExpect(method(HttpMethod.GET)).andExpect(header(HttpHeaders.AUTHORIZATION,"Bearer secret-placeholder"))
   .andRespond(withSuccess("{\"results\":[{\"symbol\":\"PETR4\",\"historicalDataPrice\":[{\"date\":\"2026-08-20\",\"close\":32.47}]}]}",MediaType.APPLICATION_JSON));
  assertEquals(new BigDecimal("32.47"),http.consultarFechamento("PETR4",date).close());server.verify();
 }
 @Test void returnsExactRawCloseAndIgnoresAdjustedClose() throws Exception {
  var value=adapter.parse(node("{\"results\":[{\"symbol\":\"PETR4\",\"historicalDataPrice\":[{\"date\":\"2026-08-20\",\"close\":32.47,\"adjustedClose\":999}]}]}"),"PETR4",date);
  assertEquals(new BigDecimal("32.47"),value.close());assertEquals(date,value.dataPregao());
 }
 @Test void rejectsWrongMissingOrMultipleTickerResults() throws Exception {
  invalid("{\"results\":[{\"symbol\":\"VALE3\",\"historicalDataPrice\":[]}]}");
  invalid("{\"results\":[{\"historicalDataPrice\":[]}]}");
  invalid("{\"results\":[{},{}]}");
 }
 @Test void rejectsMissingNullOrEmptyPrices() throws Exception {
  invalid("{\"results\":[{\"symbol\":\"PETR4\"}]}");
  invalid("{\"results\":[{\"symbol\":\"PETR4\",\"historicalDataPrice\":null}]}");
  invalid("{\"results\":[{\"symbol\":\"PETR4\",\"historicalDataPrice\":[]}]}");
 }
 @Test void rejectsBadOrDifferentDateAndInvalidClose() throws Exception {
  for(String candle:new String[]{"{}", "{\"date\":\"bad\",\"close\":1}","{\"date\":\"2026-08-19\",\"close\":1}",
   "{\"date\":\"2026-08-20\"}","{\"date\":\"2026-08-20\",\"close\":\"x\"}",
   "{\"date\":\"2026-08-20\",\"close\":0}","{\"date\":\"2026-08-20\",\"close\":-1}"})
   invalid("{\"results\":[{\"symbol\":\"PETR4\",\"historicalDataPrice\":["+candle+"]}]}");
 }
 @Test void classifiesHttpErrorsAndTransportFailuresWithoutNetwork(){
  assertHttp(HttpStatus.NOT_FOUND,ErrorCodes.TICKER_INEXISTENTE);
  assertHttp(HttpStatus.TOO_MANY_REQUESTS,ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO);
  assertHttp(HttpStatus.SERVICE_UNAVAILABLE,ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL);
  assertHttp(HttpStatus.BAD_REQUEST,ErrorCodes.RESPOSTA_EXTERNA_INVALIDA);
  RestClient timeoutClient=RestClient.builder().requestFactory((uri,method)->{throw new HttpTimeoutException("timeout");}).build();
  assertCode(ErrorCodes.SERVICO_EXTERNO_TIMEOUT,()->new BrapiCotacaoHistoricaAdapter(timeoutClient,"key").consultarFechamento("PETR4",date));
  RestClient unavailableClient=RestClient.builder().requestFactory((uri,method)->{throw new java.net.ConnectException("offline");}).build();
  assertCode(ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,()->new BrapiCotacaoHistoricaAdapter(unavailableClient,"key").consultarFechamento("PETR4",date));
  assertCode(ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,()->new BrapiCotacaoHistoricaAdapter(RestClient.create()," ").consultarFechamento("PETR4",date));
 }
 @Test void classifiesOnlyUnequivocalProviderMessages() throws Exception {
  JsonNode rateLimit=node("{\"message\":\"Rate limit exceeded\"}");
  JsonNode tickerMissing=node("{\"message\":\"Symbol PETR4 not found\"}");
  assertCode(ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO,()->adapter.parse(rateLimit,"PETR4",date));
  assertCode(ErrorCodes.TICKER_INEXISTENTE,()->adapter.parse(tickerMissing,"PETR4",date));
  for(String message:new String[]{"Invalid request","Provider error","API unavailable"})
   invalid("{\"message\":\""+message+"\"}");
 }
 private void assertHttp(HttpStatus status,String code){
  RestClient.Builder builder=RestClient.builder().baseUrl("http://brapi.test");MockRestServiceServer local=MockRestServiceServer.bindTo(builder).build();
  local.expect(requestTo("http://brapi.test/api/v2/stocks/historical?symbols=PETR4&startDate=2026-08-20&endDate=2026-08-20&interval=1d"))
   .andRespond(withStatus(status));
  assertCode(code,()->new BrapiCotacaoHistoricaAdapter(builder.build(),"key").consultarFechamento("PETR4",date));local.verify();
 }
 private void assertCode(String code,Runnable call){ApiException e=assertThrows(ApiException.class,call::run);assertEquals(code,e.getCode());}
 private void invalid(String body)throws Exception{ApiException e=assertThrows(ApiException.class,()->adapter.parse(node(body),"PETR4",date));assertEquals(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,e.getCode());}
 private JsonNode node(String body)throws Exception{return json.readTree(body);}
}
