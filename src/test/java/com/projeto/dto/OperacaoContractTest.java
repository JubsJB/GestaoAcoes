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
 @Test void bindsValidPurchaseWithoutPrice() throws Exception {
  OperacaoCreateRequest value=json.readValue(valid("COMPRA",""),OperacaoCreateRequest.class);
  assertInstanceOf(OperacaoCompraCreateRequest.class,value);assertTrue(validator.validate(value).isEmpty());
 }
 @Test void bindsValidSaleWithRequiredPrice() throws Exception {
  OperacaoCreateRequest value=json.readValue(valid("VENDA",",\"precoUnitario\":10.25"),OperacaoCreateRequest.class);
  assertInstanceOf(OperacaoVendaCreateRequest.class,value);assertTrue(validator.validate(value).isEmpty());
 }
 @Test void rejectsPriceEvenNullOnPurchase(){
  assertThrows(Exception.class,()->json.readValue(valid("COMPRA",",\"precoUnitario\":10"),OperacaoCreateRequest.class));
  assertThrows(Exception.class,()->json.readValue(valid("COMPRA",",\"precoUnitario\":null"),OperacaoCreateRequest.class));
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
