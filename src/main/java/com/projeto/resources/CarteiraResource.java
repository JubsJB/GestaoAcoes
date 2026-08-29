package com.projeto.resources;

import com.projeto.dto.CarteiraCreateRequest;
import com.projeto.dto.CarteiraResponse;
import com.projeto.dto.CarteiraUpdateRequest;
import com.projeto.dto.EvolucaoPatrimonialResponse;
import com.projeto.dto.OperacaoResponse;
import com.projeto.dto.PatrimonioResponse;
import com.projeto.dto.PosicaoResponse;
import com.projeto.dto.ResultadoRealizadoResponse;
import com.projeto.dto.ResumoCarteiraResponse;
import com.projeto.dto.SnapshotCarteiraResponse;
import com.projeto.resources.exceptions.StandardError;
import com.projeto.services.CarteiraService;
import com.projeto.services.EvolucaoPatrimonialService;
import com.projeto.services.OperacaoService;
import com.projeto.services.PatrimonioService;
import com.projeto.services.PosicaoService;
import com.projeto.services.ResultadoRealizadoService;
import com.projeto.services.ResumoCarteiraService;
import com.projeto.services.SnapshotCarteiraService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/carteiras")
public class CarteiraResource {

    private final CarteiraService service;
    private final OperacaoService operacaoService;
    private final PosicaoService posicaoService;
    private final ResultadoRealizadoService resultadoRealizadoService;
    private final PatrimonioService patrimonioService;
    private final ResumoCarteiraService resumoCarteiraService;
    private final SnapshotCarteiraService snapshotCarteiraService;
    private final EvolucaoPatrimonialService evolucaoPatrimonialService;

    public CarteiraResource(
            CarteiraService service,
            OperacaoService operacaoService,
            PosicaoService posicaoService,
            ResultadoRealizadoService resultadoRealizadoService,
            PatrimonioService patrimonioService,
            ResumoCarteiraService resumoCarteiraService,
            SnapshotCarteiraService snapshotCarteiraService,
            EvolucaoPatrimonialService evolucaoPatrimonialService
    ) {
        this.service = service;
        this.operacaoService = operacaoService;
        this.posicaoService = posicaoService;
        this.resultadoRealizadoService = resultadoRealizadoService;
        this.patrimonioService = patrimonioService;
        this.resumoCarteiraService = resumoCarteiraService;
        this.snapshotCarteiraService = snapshotCarteiraService;
        this.evolucaoPatrimonialService = evolucaoPatrimonialService;
    }

    @PostMapping
    @Operation(summary = "Criar carteira", tags = "Carteiras")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Carteira criada", content = @Content(schema = @Schema(implementation = CarteiraResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request inválido", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "409", description = "INTEGRIDADE_DADOS_VIOLADA", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<CarteiraResponse> cadastrar(@Valid @RequestBody CarteiraCreateRequest request) {
        CarteiraResponse response = service.cadastrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar carteiras", tags = "Carteiras")
    @ApiResponse(responseCode = "200", description = "Carteiras cadastradas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CarteiraResponse.class))))
    public ResponseEntity<List<CarteiraResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar carteira por ID", tags = "Carteiras")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carteira encontrada", content = @Content(schema = @Schema(implementation = CarteiraResponse.class))),
            @ApiResponse(responseCode = "404", description = "Carteira não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<CarteiraResponse> buscarPorId(
            @Parameter(description = "Identificador da carteira", example = "1", required = true) @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/{carteiraId}/operacoes")
    @Operation(summary = "Listar operações da carteira", description = "Retorna o histórico na ordenação cronológica definida pelo domínio.", tags = "Operações")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operações da carteira", content = @Content(array = @ArraySchema(schema = @Schema(implementation = OperacaoResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Carteira não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<List<OperacaoResponse>> listarOperacoes(
            @Parameter(description = "Identificador da carteira", example = "1", required = true) @PathVariable Long carteiraId
    ) {
        return ResponseEntity.ok(operacaoService.listarPorCarteira(carteiraId));
    }

    @GetMapping("/{carteiraId}/posicoes")
    @Operation(summary = "Listar posições abertas da carteira", tags = "Indicadores da Carteira")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posições consolidadas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PosicaoResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Carteira não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "422", description = "Histórico inconsistente ou cálculo fora da precisão", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<List<PosicaoResponse>> listarPosicoes(
            @Parameter(description = "Identificador da carteira", example = "1", required = true) @PathVariable Long carteiraId
    ) {
        return ResponseEntity.ok(posicaoService.listarPorCarteira(carteiraId));
    }

    @GetMapping("/{carteiraId}/resultados-realizados")
    @Operation(summary = "Consultar resultados realizados da carteira", tags = "Indicadores da Carteira")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados realizados por ação", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResultadoRealizadoResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Carteira não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "422", description = "Histórico inconsistente ou cálculo fora da precisão", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<List<ResultadoRealizadoResponse>> listarResultadosRealizados(
            @Parameter(description = "Identificador da carteira", example = "1", required = true) @PathVariable Long carteiraId
    ) {
        return ResponseEntity.ok(resultadoRealizadoService.listarPorCarteira(carteiraId));
    }

    @GetMapping("/{carteiraId}/patrimonio")
    @Operation(summary = "Consultar patrimônio atual da carteira", description = "Agrega as posições por moeda sem conversão entre BRL e USD.", tags = "Indicadores da Carteira")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patrimônio por moeda", content = @Content(schema = @Schema(implementation = PatrimonioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Carteira não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "422", description = "Histórico inconsistente ou cálculo fora da precisão", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<PatrimonioResponse> consultarPatrimonio(
            @Parameter(description = "Identificador da carteira", example = "1", required = true) @PathVariable Long carteiraId
    ) {
        return ResponseEntity.ok(patrimonioService.consultar(carteiraId));
    }

    @GetMapping("/{carteiraId}/resumo")
    @Operation(summary = "Consultar resumo atual da carteira", description = "Retorna custo, patrimônio, resultado não realizado e rentabilidade separados por moeda.", tags = "Indicadores da Carteira")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo por moeda", content = @Content(schema = @Schema(implementation = ResumoCarteiraResponse.class))),
            @ApiResponse(responseCode = "404", description = "Carteira não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "422", description = "Histórico inconsistente ou cálculo fora da precisão", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<ResumoCarteiraResponse> consultarResumo(
            @Parameter(description = "Identificador da carteira", example = "1", required = true) @PathVariable Long carteiraId
    ) {
        return ResponseEntity.ok(resumoCarteiraService.consultar(carteiraId));
    }

    @PostMapping("/{carteiraId}/snapshots")
    @Operation(summary = "Criar snapshot da carteira", description = "Persiste explicitamente o patrimônio atual separado por moeda.", tags = "Indicadores da Carteira")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Snapshot criado", content = @Content(schema = @Schema(implementation = SnapshotCarteiraResponse.class))),
            @ApiResponse(responseCode = "404", description = "Carteira não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "409", description = "SNAPSHOT_CARTEIRA_DUPLICADO ou INTEGRIDADE_DADOS_VIOLADA", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "422", description = "Histórico inconsistente ou cálculo fora da precisão", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<SnapshotCarteiraResponse> criarSnapshot(
            @Parameter(description = "Identificador da carteira", example = "1", required = true) @PathVariable Long carteiraId
    ) {
        SnapshotCarteiraResponse response = snapshotCarteiraService.criar(carteiraId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{snapshotId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{carteiraId}/evolucao-patrimonial")
    @Operation(summary = "Consultar evolução patrimonial", description = "Retorna todos os snapshots persistidos, do mais antigo ao mais recente, sem reconstrução retroativa.", tags = "Indicadores da Carteira")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Série histórica persistida", content = @Content(schema = @Schema(implementation = EvolucaoPatrimonialResponse.class))),
            @ApiResponse(responseCode = "404", description = "Carteira não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<EvolucaoPatrimonialResponse> consultarEvolucaoPatrimonial(
            @Parameter(description = "Identificador da carteira", example = "1", required = true) @PathVariable Long carteiraId
    ) {
        return ResponseEntity.ok(evolucaoPatrimonialService.consultar(carteiraId));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar nome da carteira", tags = "Carteiras")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carteira atualizada", content = @Content(schema = @Schema(implementation = CarteiraResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request inválido", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "404", description = "Carteira não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<CarteiraResponse> atualizar(
            @Parameter(description = "Identificador da carteira", example = "1", required = true) @PathVariable Long id,
            @Valid @RequestBody CarteiraUpdateRequest request
    ) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir carteira", description = "Exclui somente carteiras sem operações e sem snapshots.", tags = "Carteiras")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Carteira excluída"),
            @ApiResponse(responseCode = "404", description = "Carteira não encontrada", content = @Content(schema = @Schema(implementation = StandardError.class))),
            @ApiResponse(responseCode = "409", description = "CARTEIRA_POSSUI_OPERACOES, CARTEIRA_POSSUI_SNAPSHOTS ou INTEGRIDADE_DADOS_VIOLADA", content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    public ResponseEntity<Void> excluir(
            @Parameter(description = "Identificador da carteira", example = "1", required = true) @PathVariable Long id
    ) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
