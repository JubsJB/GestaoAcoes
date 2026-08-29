package com.projeto.resources;

import com.projeto.dto.CorretoraCreateRequest;
import com.projeto.dto.CorretoraResponse;
import com.projeto.resources.exceptions.StandardError;
import com.projeto.services.CorretoraService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/corretoras")
@Tag(name = "Corretoras", description = "Cadastro e consultas de corretoras")
public class CorretoraResource {

    private final CorretoraService service;

    public CorretoraResource(CorretoraService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cadastrar corretora", description = "Valida CNPJ e CEP nos providers configurados e persiste a corretora.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Corretora cadastrada", content = @Content(schema = @Schema(implementation = CorretoraResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request, CNPJ ou CEP inválido", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "409", description = "CORRETORA_DUPLICADA, SITUACAO_CADASTRAL_NAO_ATIVA ou INTEGRIDADE_DADOS_VIOLADA", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "422", description = "Dados externos incompletos ou situação cadastral não confirmada", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "429", description = "Limite do provider excedido", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "502", description = "Resposta externa inválida", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "503", description = "Provider externo indisponível", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "504", description = "Timeout do provider externo", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<CorretoraResponse> cadastrar(@Valid @RequestBody CorretoraCreateRequest request) {
        CorretoraResponse response = service.cadastrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar corretoras")
    @ApiResponse(responseCode = "200", description = "Corretoras cadastradas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CorretoraResponse.class))))
    public ResponseEntity<List<CorretoraResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar corretora por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Corretora encontrada", content = @Content(schema = @Schema(implementation = CorretoraResponse.class))),
            @ApiResponse(responseCode = "404", description = "Corretora não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<CorretoraResponse> buscarPorId(
            @Parameter(description = "Identificador da corretora", example = "1", required = true) @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/por-cnpj")
    @Operation(summary = "Consultar corretora por CNPJ", description = "Aceita CNPJ com ou sem máscara e consulta somente os dados persistidos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Corretora encontrada", content = @Content(schema = @Schema(implementation = CorretoraResponse.class))),
            @ApiResponse(responseCode = "400", description = "CNPJ ausente ou inválido", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "404", description = "Corretora não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<CorretoraResponse> buscarPorCnpj(
            @Parameter(description = "CNPJ com ou sem máscara", example = "12.345.678/0001-90", required = true)
            @RequestParam(required = false) String cnpj
    ) {
        return ResponseEntity.ok(service.buscarPorCnpj(cnpj));
    }
}
