package com.projeto.integrations.cnpj;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrasilApiAdapterTest {

    private MockRestServiceServer server;
    private BrasilApiAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://brasil-api.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new BrasilApiAdapter(builder.build());
    }

    @Test
    void mapsSuccessfulResponse() {
        server.expect(requestTo("http://brasil-api.test/api/cnpj/v1/11222333000181"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "cnpj": "11.222.333/0001-81",
                          "razao_social": "Corretora Exemplo S.A.",
                          "nome_fantasia": "Exemplo",
                          "email": "contato@exemplo.test",
                          "ddd_telefone_1": "1130000000",
                          "cep": "01001-000",
                          "numero": "100",
                          "complemento": "10 andar",
                          "descricao_situacao_cadastral": "ATIVA"
                        }
                        """, org.springframework.http.MediaType.APPLICATION_JSON));

        CnpjData result = adapter.consultar("11222333000181");

        assertEquals("Corretora Exemplo S.A.", result.razaoSocial());
        assertEquals("01001-000", result.cep());
        assertEquals("ATIVA", result.situacaoCadastral());
        server.verify();
    }

    @Test
    void mapsNotFoundRateLimitUnavailableAndInvalidPayload() {
        assertMappedError(HttpStatus.NOT_FOUND, ErrorCodes.CNPJ_INEXISTENTE, "{}");
        assertMappedError(HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO, "{}");
        assertMappedError(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL, "{}");

        server.expect(requestTo("http://brasil-api.test/api/cnpj/v1/11222333000181"))
                .andRespond(withSuccess("{", org.springframework.http.MediaType.APPLICATION_JSON));
        ApiException malformed = assertThrows(ApiException.class, () -> adapter.consultar("11222333000181"));
        assertEquals(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA, malformed.getCode());
        server.verify();
    }

    @Test
    void mapsTimeoutWithoutCallingARealService() {
        RestClient timeoutClient = RestClient.builder()
                .requestFactory((uri, method) -> {
                    throw new HttpTimeoutException("timeout");
                })
                .build();

        ApiException exception = assertThrows(
                ApiException.class,
                () -> new BrasilApiAdapter(timeoutClient).consultar("11222333000181")
        );

        assertEquals(ErrorCodes.SERVICO_EXTERNO_TIMEOUT, exception.getCode());
        assertEquals(504, exception.getStatus().value());
    }

    @Test
    void exposesMissingFieldsFromIncompletePayloadForServiceValidation() {
        server.expect(requestTo("http://brasil-api.test/api/cnpj/v1/11222333000181"))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));

        CnpjData result = adapter.consultar("11222333000181");

        assertNull(result.cnpj());
        assertNull(result.razaoSocial());
        assertNull(result.cep());
        assertNull(result.situacaoCadastral());
        server.verify();
    }

    private void assertMappedError(HttpStatus status, String expectedCode, String body) {
        server.expect(requestTo("http://brasil-api.test/api/cnpj/v1/11222333000181"))
                .andRespond(withStatus(status).body(body));
        ApiException exception = assertThrows(ApiException.class, () -> adapter.consultar("11222333000181"));
        assertEquals(expectedCode, exception.getCode());
        server.verify();
        server.reset();
    }
}
