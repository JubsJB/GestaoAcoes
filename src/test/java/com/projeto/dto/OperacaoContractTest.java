package com.projeto.dto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class OperacaoContractTest {
 private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
 private Validator validator;
 @BeforeEach void setup(){validator=Validation.buildDefaultValidatorFactory().getValidator();}
 @Test void bindsValidPurchaseWithRequiredPrice() throws Exception {
  OperacaoCreateRequest value=json.readValue(valid("COMPRA",",\"precoUnitario\":10.25"),OperacaoCreateRequest.class);
  assertInstanceOf(OperacaoCompraCreateRequest.class,value);assertTrue(validator.validate(value).isEmpty());
 }
 @Test void bindsValidSaleWithRequiredPrice() throws Exception {
  OperacaoCreateRequest value=json.readValue(valid("VENDA",",\"precoUnitario\":10.25"),OperacaoCreateRequest.class);
  assertInstanceOf(OperacaoVendaCreateRequest.class,value);assertTrue(validator.validate(value).isEmpty());
 }
 @Test void purchaseMissingNullOrNonPositivePriceFailsValidation() throws Exception {
  for(String extra:new String[]{"",",\"precoUnitario\":null",",\"precoUnitario\":0",",\"precoUnitario\":-1"}) {
   var value=json.readValue(valid("COMPRA",extra),OperacaoCreateRequest.class);
   assertTrue(validator.validate(value).stream().anyMatch(e->e.getPropertyPath().toString().equals("precoUnitario")));
  }
 }
 @Test void saleWithoutPriceFailsValidation() throws Exception {
  Set<ConstraintViolation<OperacaoCreateRequest>> errors=validator.validate(json.readValue(valid("VENDA",""),OperacaoCreateRequest.class));
  assertTrue(errors.stream().anyMatch(e->e.getPropertyPath().toString().equals("precoUnitario")));
 }
 @Test void rejectsOrderAndUnknownFieldsForBothVariants(){
  for(String type:new String[]{"COMPRA","VENDA"})for(String extra:new String[]{",\"ordemNoDia\":1",",\"surpresa\":true"})
   assertThrows(Exception.class,()->json.readValue(valid(type,(type.equals("VENDA")?",\"precoUnitario\":10":"")+extra),OperacaoCreateRequest.class));
 }
 @Test void rejectsMissingNullUnknownAndWrongCaseDiscriminator(){
  for(String body:new String[]{common().replace(",\"tipo\":\"COMPRA\"",""),common().replace("\"COMPRA\"","null"),
    common().replace("COMPRA","OUTRO"),common().replace("COMPRA","compra")})
   assertThrows(Exception.class,()->json.readValue(body,OperacaoCreateRequest.class));
 }
 @Test void brokerMayBeOmittedOrNull() throws Exception {
  assertNull(json.readValue(valid("COMPRA",""),OperacaoCreateRequest.class).getCorretoraId());
  assertNull(json.readValue(valid("COMPRA",",\"corretoraId\":null"),OperacaoCreateRequest.class).getCorretoraId());
 }
 private String valid(String type,String extra){return common().replace("COMPRA",type).replace("}",extra+"}");}
 private String common(){return "{\"carteiraId\":1,\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\",\"tipo\":\"COMPRA\",\"quantidade\":10,\"dataOperacao\":\"2026-08-20\"}";}
}
