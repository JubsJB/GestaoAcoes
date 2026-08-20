package com.projeto.integrations.cep;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ViaCepAdapterTest {

    private MockRestServiceServer server;
    private ViaCepAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://via-cep.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new ViaCepAdapter(builder.build());
    }

    @Test
    void mapsSuccessfulResponse() {
        server.expect(requestTo("http://via-cep.test/ws/01001000/json/"))
                .andRespond(withSuccess("""
                        {
                          "cep": "01001-000",
                          "logradouro": "Praca da Se",
                          "bairro": "Se",
                          "localidade": "Sao Paulo",
                          "uf": "SP"
                        }
                        """, org.springframework.http.MediaType.APPLICATION_JSON));

        CepData result = adapter.consultar("01001000");

        assertEquals("Praca da Se", result.logradouro());
        assertEquals("Sao Paulo", result.cidade());
        server.verify();
    }

    @Test
    void mapsLogicalNotFoundHttpErrorsAndMalformedPayload() {
        server.expect(requestTo("http://via-cep.test/ws/01001000/json/"))
                .andRespond(withSuccess("{\"erro\":true}", org.springframework.http.MediaType.APPLICATION_JSON));
        assertCode(ErrorCodes.CEP_INEXISTENTE);
        server.reset();

        assertMappedError(HttpStatus.NOT_FOUND, ErrorCodes.CEP_INEXISTENTE);
        assertMappedError(HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO);
        assertMappedError(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL);

        server.expect(requestTo("http://via-cep.test/ws/01001000/json/"))
                .andRespond(withSuccess("{", org.springframework.http.MediaType.APPLICATION_JSON));
        assertCode(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA);
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
                () -> new ViaCepAdapter(timeoutClient).consultar("01001000")
        );

        assertEquals(ErrorCodes.SERVICO_EXTERNO_TIMEOUT, exception.getCode());
        assertEquals(504, exception.getStatus().value());
    }

    @Test
    void exposesMissingFieldsFromIncompletePayloadForServiceValidation() {
        server.expect(requestTo("http://via-cep.test/ws/01001000/json/"))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));

        CepData result = adapter.consultar("01001000");

        assertNull(result.cep());
        assertNull(result.logradouro());
        assertNull(result.bairro());
        assertNull(result.cidade());
        assertNull(result.uf());
        server.verify();
    }

    private void assertMappedError(HttpStatus status, String expectedCode) {
        server.expect(requestTo("http://via-cep.test/ws/01001000/json/"))
                .andRespond(withStatus(status));
        assertCode(expectedCode);
        server.verify();
        server.reset();
    }

    private void assertCode(String expectedCode) {
        ApiException exception = assertThrows(ApiException.class, () -> adapter.consultar("01001000"));
        assertEquals(expectedCode, exception.getCode());
    }
}
