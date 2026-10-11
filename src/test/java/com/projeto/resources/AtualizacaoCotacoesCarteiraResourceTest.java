package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.*;
import com.projeto.services.*;
import com.projeto.services.exceptions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.projeto.services.RegraDecisaoCotacao.Resultado.*;

@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AtualizacaoCotacoesCarteiraResourceTest {
    static final String PATH = "/carteiras/1/atualizacoes-cotacoes";
    static final OffsetDateTime TIME = OffsetDateTime.parse("2026-10-04T15:04:00Z");
    @Autowired MockMvc mvc;
    @MockitoBean AtualizacaoCotacoesCarteiraService service;
    @MockitoBean AcaoService individual;
    @MockitoBean AplicacaoCotacaoService aplicacao;

    ResultadoAtualizacaoCotacao item(long id, RegraDecisaoCotacao.Resultado resultado,
                                     ReferenciaTipoCotacao tipo) {
        var erro = resultado == FALHA ? new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                "SERVICO_EXTERNO_TIMEOUT", "Serviço externo demorou a responder",
                Map.of("sql", "SEGREDO_INTERNO", "payload", "TOKEN_INTERNO")) : null;
        if (erro != null) erro.initCause(new IllegalStateException("CAUSA_INTERNA"));
        return new ResultadoAtualizacaoCotacao(id, "ATIVO" + id, "Empresa", Mercado.EUA, Moeda.USD,
                resultado, resultado == ATUALIZADA ? "REFERENCIA_POSTERIOR"
                : resultado == SEM_NOVA_COTACAO ? "MESMA_OBSERVACAO" : erro.getCode(),
                ResultadoAtualizacaoCotacao.Origem.CONSULTA,
                new AplicacaoCotacaoService.EstadoCotacao(new BigDecimal("9999999999999.999999"), tipo,
                        tipo == ReferenciaTipoCotacao.INSTANTE ? TIME.minusDays(1) : null,
                        tipo == ReferenciaTipoCotacao.DATA ? LocalDate.of(2026,10,2) : null,
                        tipo == ReferenciaTipoCotacao.DESCONHECIDA ? null : TIME, TIME.plusDays(3)), erro);
    }

    void responder(List<ResultadoAtualizacaoCotacao> itens) {
        long atualizadas = itens.stream().filter(i -> i.resultado() == ATUALIZADA).count();
        long semNova = itens.stream().filter(i -> i.resultado() == SEM_NOVA_COTACAO).count();
        when(service.atualizar(1L)).thenReturn(new AtualizacaoCotacoesCarteiraService.Lote(1L,
                new AtualizacaoCotacoesCarteiraService.Resumo(atualizadas, semNova, itens.size()-atualizadas-semNova), itens));
    }

    @Test void semBodyVazio200() throws Exception {
        responder(List.of());
        mvc.perform(post(PATH)).andExpect(status().isOk())
                .andExpect(jsonPath("$.carteiraId").value(1))
                .andExpect(jsonPath("$.resultados", hasSize(0)))
                .andExpect(jsonPath("$.resumo.atualizadas").value(0))
                .andExpect(jsonPath("$.resumo.semNovaCotacao").value(0))
                .andExpect(jsonPath("$.resumo.falhas").value(0));
        verify(service).atualizar(1L);
        verifyNoInteractions(individual, aplicacao);
    }

    @ParameterizedTest @ValueSource(strings={"{}", "{\"acaoIds\":[1]}", "[]", "[1,2]", "null", "42", "\"texto\"", "{invalido"})
    void bodyRejeitadoAntesDeProcessar(String body) throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));
        verifyNoInteractions(service, individual, aplicacao);
    }

    @Test void conteudoSemConversorTambem400() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.TEXT_PLAIN).content("conteúdo"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));
        verifyNoInteractions(service, individual, aplicacao);
    }

    @Test void inexistenteMantem404() throws Exception {
        when(service.atualizar(1L)).thenThrow(new ObjectNotFoundException("Carteira não encontrada para o id: 1"));
        mvc.perform(post(PATH)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Carteira não encontrada para o id: 1"))
                .andExpect(jsonPath("$.path").value(PATH));
    }

    @Test void replayMantemErroConhecido() throws Exception {
        when(service.atualizar(1L)).thenThrow(new ApiException(HttpStatus.CONFLICT,
                ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE,"Histórico de Operações inconsistente"));
        mvc.perform(post(PATH)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE));
    }

    @Test void integridadeGlobalConhecidaMantem409Seguro() throws Exception {
        when(service.atualizar(1L)).thenThrow(new org.springframework.dao.DataIntegrityViolationException("SQL segredo"));
        mvc.perform(post(PATH)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INTEGRIDADE_DADOS_VIOLADA"))
                .andExpect(content().string(not(containsString("segredo"))));
    }

    @Test void precisaoReplayMantem422() throws Exception {
        when(service.atualizar(1L)).thenThrow(new ApiException(HttpStatus.valueOf(422),
                ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO,"Cálculo da posição excede a precisão aprovada"));
        mvc.perform(post(PATH)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO));
    }

    @Test void parcialMapeiaTresCategoriasSemInternos() throws Exception {
        responder(List.of(item(2,ATUALIZADA,ReferenciaTipoCotacao.INSTANTE),
                item(5,SEM_NOVA_COTACAO,ReferenciaTipoCotacao.DATA), item(9,FALHA,ReferenciaTipoCotacao.DESCONHECIDA)));
        mvc.perform(post(PATH)).andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.atualizadas").value(1))
                .andExpect(jsonPath("$.resumo.semNovaCotacao").value(1))
                .andExpect(jsonPath("$.resumo.falhas").value(1))
                .andExpect(jsonPath("$.resultados[*].acaoId", contains(2,5,9)))
                .andExpect(jsonPath("$.resultados[*].resultado", contains("ATUALIZADA","SEM_NOVA_COTACAO","FALHA")))
                .andExpect(jsonPath("$.resultados[2].erro", aMapWithSize(2)))
                .andExpect(jsonPath("$.resultados[2].erro.code").value("SERVICO_EXTERNO_TIMEOUT"))
                .andExpect(jsonPath("$.resultados[0].erro").value(nullValue()))
                .andExpect(content().string(not(containsString("SEGREDO_INTERNO"))))
                .andExpect(content().string(not(containsString("TOKEN_INTERNO"))))
                .andExpect(content().string(not(containsString("CAUSA_INTERNA"))))
                .andExpect(content().string(not(containsString("stackTrace"))))
                .andExpect(content().string(not(containsString("\"falha\""))))
                .andExpect(content().string(not(containsString("\"origem\""))))
                .andExpect(content().string(not(containsString("\"details\""))));
    }

    @ParameterizedTest @EnumSource(RegraDecisaoCotacao.Resultado.class)
    void todasCategoriasRetornam200(RegraDecisaoCotacao.Resultado resultado) throws Exception {
        responder(List.of(item(1,resultado,ReferenciaTipoCotacao.DATA),item(2,resultado,ReferenciaTipoCotacao.DATA)));
        mvc.perform(post(PATH)).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultados[*].resultado", contains(resultado.name(),resultado.name())))
                .andExpect(jsonPath("$.resumo.falhas").value(resultado==FALHA?2:0));
    }

    @ParameterizedTest @EnumSource(ReferenciaTipoCotacao.class)
    void snapshotTemporalFielComPrecisaoExata(ReferenciaTipoCotacao tipo) throws Exception {
        responder(List.of(item(1,SEM_NOVA_COTACAO,tipo)));
        mvc.perform(post(PATH)).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultados[0].cotacao.referenciaMercadoCotacao.tipo").value(tipo.name()))
                .andExpect(jsonPath("$.resultados[0].cotacao.referenciaMercadoCotacao.instante")
                        .value(tipo==ReferenciaTipoCotacao.INSTANTE?"2026-10-03T15:04:00Z":null))
                .andExpect(jsonPath("$.resultados[0].cotacao.referenciaMercadoCotacao.data")
                        .value(tipo==ReferenciaTipoCotacao.DATA?"2026-10-02":null))
                .andExpect(jsonPath("$.resultados[0].cotacao.cotacaoObtidaEm")
                        .value(tipo==ReferenciaTipoCotacao.DESCONHECIDA?null:"2026-10-04T15:04:00Z"))
                .andExpect(jsonPath("$.resultados[0].cotacao.dataHoraCotacao").value("2026-10-07T15:04:00Z"))
                .andExpect(content().string(containsString("\"cotacaoAtual\":9999999999999.999999")));
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void inesperadaInclusiveInvarianteInternaVira500Seguro(boolean argumento) throws Exception {
        when(service.atualizar(1L)).thenThrow(argumento ? new IllegalArgumentException("SQL TOKEN")
                : new IllegalStateException("SQL TOKEN"));
        mvc.perform(post(PATH)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("FALHA_ATUALIZACAO_COTACOES"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Não foi possível concluir o lote. Atualizações já confirmadas podem ter sido preservadas."))
                .andExpect(jsonPath("$.details", anEmptyMap()))
                .andExpect(content().string(not(containsString("TOKEN"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
        verify(service,times(1)).atualizar(1L);
    }
}
