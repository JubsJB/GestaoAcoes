package com.projeto.resources;

import com.projeto.dto.OperacaoCreateRequest;
import com.projeto.dto.OperacaoResponse;
import com.projeto.resources.exceptions.StandardError;
import com.projeto.services.OperacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/operacoes")
@Tag(name = "Operações", description = "Registro e consulta de compras e vendas")
public class OperacaoResource {

    private final OperacaoService service;

    public OperacaoResource(OperacaoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registrar operação", description = "Registra compra ou venda e valida cronologia, ordem no dia e posição disponível.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Operação registrada", content = @Content(schema = @Schema(implementation = OperacaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request inválido", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "404", description = "Carteira, ação ou corretora não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "409", description = "ORDEM_OPERACAO_DUPLICADA ou INTEGRIDADE_DADOS_VIOLADA", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "422", description = "Posição insuficiente, histórico inconsistente ou precisão inválida", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<OperacaoResponse> cadastrar(@Valid @RequestBody OperacaoCreateRequest request) {
        OperacaoResponse response = service.cadastrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar operações", description = "Retorna o histórico na ordenação cronológica definida pelo domínio.")
    @ApiResponse(responseCode = "200", description = "Operações registradas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = OperacaoResponse.class))))
    public ResponseEntity<List<OperacaoResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar operação por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação encontrada", content = @Content(schema = @Schema(implementation = OperacaoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Operação não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<OperacaoResponse> buscarPorId(
            @Parameter(description = "Identificador da operação", example = "1", required = true) @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
}
