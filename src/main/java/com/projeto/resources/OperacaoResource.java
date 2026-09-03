package com.projeto.resources;

import com.projeto.dto.OperacaoCreateRequest;
import com.projeto.dto.OperacaoResponse;
import com.projeto.dto.PreviaPrecoCompraResponse;
import com.projeto.resources.exceptions.StandardError;
import com.projeto.services.OperacaoService;
import com.projeto.services.PrecoOperacaoService;
import com.projeto.entities.Mercado;
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
import java.time.LocalDate;

@RestController
@RequestMapping("/operacoes")
@Tag(name = "Operações", description = "Registro e consulta de compras e vendas")
public class OperacaoResource {

    private final OperacaoService service;
    private final PrecoOperacaoService precoService;

    public OperacaoResource(OperacaoService service, PrecoOperacaoService precoService) {
        this.service = service;
        this.precoService = precoService;
    }

    @PostMapping
    @Operation(summary = "Registrar operação", description = "COMPRA consulta o fechamento histórico exato; VENDA usa o preço informado. A ordem no dia é gerada pelo backend.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Operação registrada", content = @Content(schema = @Schema(implementation = OperacaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request inválido", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "404", description = "Carteira, ação ou corretora não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "409", description = "POSICAO_INSUFICIENTE ou INTEGRIDADE_DADOS_VIOLADA", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "422", description = "COTACAO_HISTORICA_INDISPONIVEL ou HISTORICO_COTACAO_FORA_DO_ALCANCE", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "429", description = "LIMITE_REQUISICOES_EXCEDIDO (somente COMPRA)", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "502", description = "RESPOSTA_EXTERNA_INVALIDA (somente COMPRA)", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "503", description = "SERVICO_EXTERNO_INDISPONIVEL (somente COMPRA)", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "504", description = "SERVICO_EXTERNO_TIMEOUT (somente COMPRA)", content = @Content(schema = @Schema(implementation = StandardError.class)))
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

    @GetMapping("/previa-compra")
    @Operation(
            summary = "Consultar prévia do preço de COMPRA",
            description = "Retorna o fechamento histórico bruto da data exata para exibição somente leitura. A prévia é informativa: POST /operacoes consulta novamente o provider e continua sem aceitar precoUnitario em COMPRA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fechamento histórico exato", content = @Content(schema = @Schema(implementation = PreviaPrecoCompraResponse.class))),
            @ApiResponse(responseCode = "400", description = "REQUEST_INVALIDO", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "404", description = "Ação não cadastrada ou TICKER_INEXISTENTE", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "422", description = "COTACAO_HISTORICA_INDISPONIVEL ou HISTORICO_COTACAO_FORA_DO_ALCANCE", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "429", description = "LIMITE_REQUISICOES_EXCEDIDO", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "502", description = "RESPOSTA_EXTERNA_INVALIDA", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "503", description = "SERVICO_EXTERNO_INDISPONIVEL", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "504", description = "SERVICO_EXTERNO_TIMEOUT", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<PreviaPrecoCompraResponse> consultarPreviaCompra(
            @Parameter(description = "Ticker da Ação", example = "PETR4", required = true) @RequestParam String ticker,
            @Parameter(description = "Mercado da Ação", example = "BRASIL", required = true) @RequestParam Mercado mercado,
            @Parameter(description = "Data civil exata do fechamento", example = "2026-08-20", required = true) @RequestParam LocalDate dataOperacao
    ) {
        return ResponseEntity.ok(precoService.consultarPreviaCompra(ticker, mercado, dataOperacao));
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
