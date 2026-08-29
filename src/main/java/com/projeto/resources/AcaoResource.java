package com.projeto.resources;

import com.projeto.dto.AcaoCreateRequest;
import com.projeto.dto.AcaoResponse;
import com.projeto.entities.Mercado;
import com.projeto.resources.exceptions.StandardError;
import com.projeto.services.AcaoService;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/acoes")
@Tag(name = "Ações", description = "Cadastro, consultas e atualização da cotação de ações")
public class AcaoResource {

    private final AcaoService service;

    public AcaoResource(AcaoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cadastrar ação", description = "Consulta o provider correspondente ao mercado e persiste a ação e sua cotação inicial.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ação cadastrada", content = @Content(schema = @Schema(implementation = AcaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request, ticker ou mercado inválido", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "409", description = "ACAO_DUPLICADA ou INTEGRIDADE_DADOS_VIOLADA", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "422", description = "Ticker inexistente, divergente ou dados externos inválidos", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "429", description = "Limite do provider excedido", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "502", description = "Resposta externa inválida", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "503", description = "Provider externo indisponível", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "504", description = "Timeout do provider externo", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<AcaoResponse> cadastrar(@Valid @RequestBody AcaoCreateRequest request) {
        AcaoResponse response = service.cadastrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar ações")
    @ApiResponse(responseCode = "200", description = "Ações cadastradas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AcaoResponse.class))))
    public ResponseEntity<List<AcaoResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar ação por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ação encontrada", content = @Content(schema = @Schema(implementation = AcaoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ação não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<AcaoResponse> buscarPorId(
            @Parameter(description = "Identificador da ação", example = "1", required = true) @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/por-ticker")
    @Operation(summary = "Consultar ação por ticker e mercado", description = "Consulta singular pela identidade composta persistida, sem acessar providers externos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ação encontrada", content = @Content(schema = @Schema(implementation = AcaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ticker ou mercado ausente/inválido", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "404", description = "Combinação não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<AcaoResponse> buscarPorTickerEMercado(
            @Parameter(description = "Ticker normalizado para maiúsculas", example = "PETR4", required = true) @RequestParam(required = false) String ticker,
            @Parameter(description = "Mercado da ação", example = "BRASIL", required = true, schema = @Schema(allowableValues = {"BRASIL", "EUA"})) @RequestParam(required = false) Mercado mercado
    ) {
        return ResponseEntity.ok(service.buscarPorTickerEMercado(ticker, mercado));
    }

    @PatchMapping("/{id}/cotacao")
    @Operation(summary = "Atualizar cotação da ação", description = "Consulta o provider do mercado da ação e atualiza somente quando a cotação recebida é mais recente. Não aceita corpo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cotação processada", content = @Content(schema = @Schema(implementation = AcaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Corpo não permitido", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "404", description = "Ação não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "409", description = "TICKER_CANONICO_DIVERGENTE ou INTEGRIDADE_DADOS_VIOLADA", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "422", description = "Cotação indisponível ou fora da precisão", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "429", description = "Limite do provider excedido", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "502", description = "Resposta externa inválida", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "503", description = "Provider externo indisponível", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "504", description = "Timeout do provider externo", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<AcaoResponse> atualizarCotacao(
            @Parameter(description = "Identificador da ação", example = "1", required = true) @PathVariable Long id,
            HttpServletRequest request
    ) {
        if (hasBody(request)) {
            throw invalidBody();
        }

        return ResponseEntity.ok(service.atualizarCotacao(id));
    }

    private boolean hasBody(HttpServletRequest request) {
        try {
            return request.getInputStream().read() != -1;
        } catch (IOException exception) {
            throw invalidBody();
        }
    }

    private ApiException invalidBody() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.REQUEST_INVALIDO,
                "A atualização de cotação não aceita corpo de requisição"
        );
    }
}
