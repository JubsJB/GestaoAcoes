package com.projeto.resources;

import com.projeto.dto.AtualizacaoCotacoesCarteiraResponse;
import com.projeto.mappers.AtualizacaoCotacoesCarteiraMapper;
import com.projeto.services.AtualizacaoCotacoesCarteiraService;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.projeto.resources.exceptions.StandardError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/carteiras")
@Tag(name = "Carteiras")
public class AtualizacaoCotacoesCarteiraResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtualizacaoCotacoesCarteiraResource.class);
    private final AtualizacaoCotacoesCarteiraService service;
    private final AtualizacaoCotacoesCarteiraMapper mapper;

    public AtualizacaoCotacoesCarteiraResource(AtualizacaoCotacoesCarteiraService service,
                                             AtualizacaoCotacoesCarteiraMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/{carteiraId}/atualizacoes-cotacoes")
    @Operation(summary = "Atualizar cotações das posições abertas da Carteira", description = "Não aceita request body: inclusive {} e listas de IDs são rejeitados com 400 REQUEST_INVALIDO antes do processamento. A seleção pertence exclusivamente ao backend pelo replay oficial das posições abertas, sem duplicidade e em ordem de acaoId. Processamento síncrono sequencial, sem transação global ou retry automático. Retorna 200 inclusive vazio, SEM_NOVA_COTACAO, sucesso parcial e todas as falhas isoladas; erros individuais permanecem nos itens. Não altera preços de operações nem promete cotação em tempo real. Os snapshots são por item, sem garantia de estado global simultâneo; COOLDOWN_SUCESSO apresenta o snapshot da tentativa concluída, sem releitura para atualizar a apresentação, podendo diferir do estado mais recente após atualização por outra instância.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lote concluído, inclusive parcial ou todas as falhas isoladas; vazio retorna contadores zero", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AtualizacaoCotacoesCarteiraResponse.class), examples = {
                    @ExampleObject(name = "atualizada", summary = "Observação DATA confirmada", value = """
                            {"carteiraId":1,"resumo":{"atualizadas":1,"semNovaCotacao":0,"falhas":0},"resultados":[{"acaoId":2,"ticker":"AAPL","mercado":"EUA","resultado":"ATUALIZADA","motivo":"REFERENCIA_POSTERIOR","cotacao":{"cotacaoAtual":210.123456,"referenciaMercadoCotacao":{"tipo":"DATA","instante":null,"data":"2026-10-02"},"cotacaoObtidaEm":"2026-10-04T12:00:00Z","dataHoraCotacao":"2026-10-04T12:00:00Z"},"erro":null}]}
                            """),
                    @ExampleObject(name = "semNovaCotacao", summary = "Mesma observação INSTANTE", value = """
                            {"carteiraId":1,"resumo":{"atualizadas":0,"semNovaCotacao":1,"falhas":0},"resultados":[{"acaoId":3,"ticker":"PETR4","mercado":"BRASIL","resultado":"SEM_NOVA_COTACAO","motivo":"MESMA_OBSERVACAO","cotacao":{"cotacaoAtual":32.123456,"referenciaMercadoCotacao":{"tipo":"INSTANTE","instante":"2026-10-02T20:00:00Z","data":null},"cotacaoObtidaEm":"2026-10-04T12:00:00Z","dataHoraCotacao":"2026-10-02T20:00:00Z"},"erro":null}]}
                            """),
                    @ExampleObject(name = "parcial", summary = "Atualização confirmada e falha isolada sobre legado", value = """
                            {"carteiraId":1,"resumo":{"atualizadas":1,"semNovaCotacao":0,"falhas":1},"resultados":[{"acaoId":2,"ticker":"AAPL","mercado":"EUA","resultado":"ATUALIZADA","motivo":"REFERENCIA_POSTERIOR","cotacao":{"cotacaoAtual":210.123456,"referenciaMercadoCotacao":{"tipo":"DATA","instante":null,"data":"2026-10-02"},"cotacaoObtidaEm":"2026-10-04T12:00:00Z","dataHoraCotacao":"2026-10-04T12:00:00Z"},"erro":null},{"acaoId":3,"ticker":"PETR4","mercado":"BRASIL","resultado":"FALHA","motivo":"REFERENCIA_TEMPORAL_INVALIDA","cotacao":{"cotacaoAtual":32.123456,"referenciaMercadoCotacao":{"tipo":"DESCONHECIDA","instante":null,"data":null},"cotacaoObtidaEm":null,"dataHoraCotacao":"2026-08-20T15:30:00Z"},"erro":{"code":"REFERENCIA_TEMPORAL_INVALIDA","message":"Atualização exige referência de mercado"}}]}
                            """)
            })),
            @ApiResponse(responseCode = "400", description = "REQUEST_INVALIDO: parâmetro inválido ou conteúdo funcional enviado, rejeitado antes do lote", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "404", description = "Carteira inexistente, mantendo o erro global padronizado; também aplicável a identidade ausente durante leitura", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "409", description = "Erro global conhecido: HISTORICO_OPERACOES_INCONSISTENTE no replay ou INTEGRIDADE_DADOS_VIOLADA fora de falha isolável", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "422", description = "Erro global conhecido no replay: CALCULO_POSICAO_FORA_DA_PRECISAO; falhas temporais individuais são itens FALHA no 200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "500", description = "Falha global inesperada: FALHA_ATUALIZACAO_COTACOES. Não foi possível concluir o lote. Atualizações já confirmadas podem ter sido preservadas.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class), examples = @ExampleObject(value = """
                    {"timeStamp":1791115200000,"status":500,"error":"Internal Server Error","message":"Não foi possível concluir o lote. Atualizações já confirmadas podem ter sido preservadas.","path":"/carteiras/1/atualizacoes-cotacoes","code":"FALHA_ATUALIZACAO_COTACOES","details":{}}
                    """)))
    })
    public ResponseEntity<AtualizacaoCotacoesCarteiraResponse> atualizar(
            @Parameter(description = "Identificador da Carteira; Ações selecionadas exclusivamente pelo backend", required = true, example = "1") @PathVariable Long carteiraId,
            @Parameter(hidden = true) @RequestBody(required = false) JsonNode body) {
        // O conversor HTTP lida com ausência de conteúdo; não há DTO funcional nem inspeção de bytes.
        if (body != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.REQUEST_INVALIDO,
                    "A atualização de cotações da Carteira não aceita corpo de requisição");
        }
        try {
            return ResponseEntity.ok(mapper.toResponse(service.atualizar(carteiraId)));
        } catch (ApiException | ObjectNotFoundException | DataIntegrityViolationException conhecida) {
            throw conhecida;
        } catch (RuntimeException inesperada) {
            LOGGER.error("Falha global na atualização de cotações da Carteira {}", carteiraId, inesperada);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.FALHA_ATUALIZACAO_COTACOES,
                    "Não foi possível concluir o lote. Atualizações já confirmadas podem ter sido preservadas.");
        }
    }
}
