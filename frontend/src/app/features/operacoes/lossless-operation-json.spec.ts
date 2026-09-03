import { parseOperationJson } from './lossless-operation-json';
describe('lossless operation JSON',()=>{
  it('preserva campos decimais numéricos em objetos e arrays',()=>{const value=parseOperationJson<Array<Record<string,unknown>>>('[{"id":1,"quantidade":1234567890123.123456,"precoUnitario":0.000001,"valorTotal":12345678901234567890123456.123456789012}]');expect(value[0]['quantidade']).toBe('1234567890123.123456');expect(value[0]['precoUnitario']).toBe('0.000001');expect(value[0]['valorTotal']).toBe('12345678901234567890123456.123456789012');});
  it('preserva strings, escapes, nulos e StandardError textual',()=>{const value=parseOperationJson<Record<string,unknown>>('{"message":"campo \\"quantidade\\"","quantidade":null,"details":{"precoUnitario":"inválido"}}');expect(value['message']).toBe('campo "quantidade"');expect(value['quantidade']).toBeNull();expect(value['details']).toEqual({precoUnitario:'inválido'});});
  it('preserva sugestão de venda presente ou nula',()=>{expect(parseOperationJson<{precoUnitarioSugerido:string}>('{"precoUnitarioSugerido":9999999999999.123456}').precoUnitarioSugerido).toBe('9999999999999.123456');expect(parseOperationJson<{precoUnitarioSugerido:null}>('{"precoUnitarioSugerido":null}').precoUnitarioSugerido).toBeNull();});
  it('falha explicitamente em contrato malformado',()=>expect(()=>parseOperationJson('{"quantidade":x}')).toThrow());
});
