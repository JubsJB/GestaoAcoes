package com.projeto.resources;

import com.projeto.dto.DistribuicaoCarteiraResponse;
import com.projeto.mappers.DistribuicaoCorretoraMapper;
import com.projeto.resources.exceptions.StandardError;
import com.projeto.services.DistribuicaoPosicaoCorretoraService;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/carteiras/{carteiraId}/posicoes/distribuicao-corretoras")
public class DistribuicaoCorretoraResource {
    private final DistribuicaoPosicaoCorretoraService service;
    private final DistribuicaoCorretoraMapper mapper;

    public DistribuicaoCorretoraResource(DistribuicaoPosicaoCorretoraService service,
            DistribuicaoCorretoraMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping(produces = "application/json")
    @Operation(summary = "Consultar distribuicao informativa por corretora", tags = "Carteiras",
            description = "Posicoes abertas derivadas das operacoes registradas, nao custodia comprovada. "
                    + "Negativos e deficits historicos, inclusive grupos zerados, permanecem diagnosticados. "
                    + "Quantidades decimais exatas; reconciliacao pertence ao snapshot desta resposta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribuicao, inclusive vazia ou inconsistente",
                    content = @Content(schema = @Schema(implementation = DistribuicaoCarteiraResponse.class),
                            examples = {
                                    @ExampleObject(name = "reconciliada", value = DistribuicaoCorretoraExamples.EXAMPLE_1),
                                    @ExampleObject(name = "negativoAtual", value = DistribuicaoCorretoraExamples.EXAMPLE_2),
                                    @ExampleObject(name = "deficitRegularizado", value = DistribuicaoCorretoraExamples.EXAMPLE_3),
                                    @ExampleObject(name = "zeroComDeficit", value = DistribuicaoCorretoraExamples.EXAMPLE_4),
                                    @ExampleObject(name = "divergencia", value = DistribuicaoCorretoraExamples.EXAMPLE_5)
                            })),
            @ApiResponse(responseCode = "400", description = "REQUEST_INVALIDO",
                    content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "404", description = "Carteira inexistente",
                    content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "409", description = "HISTORICO_OPERACOES_INCONSISTENTE",
                    content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "500", description = "FALHA_CONSULTA_DISTRIBUICAO_CORRETORAS",
                    content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<DistribuicaoCarteiraResponse> consultar(
            @Parameter(required = true, example = "1") @PathVariable Long carteiraId) {
        try {
            return ResponseEntity.ok(mapper.toResponse(service.consultar(carteiraId)));
        } catch (ApiException | ObjectNotFoundException conhecida) {
            throw conhecida;
        } catch (RuntimeException inesperada) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.FALHA_CONSULTA_DISTRIBUICAO_CORRETORAS,
                    "Nao foi possivel consultar a distribuicao por corretora.");
        }
    }
}
