package com.projeto.resources;

import com.projeto.dto.RegistroCustodiaResponse;
import com.projeto.dto.DetalheCustodiaResponse;
import com.projeto.dto.CustodiaCreateRequest;
import com.projeto.dto.TransferenciaCustodiaCreateRequest;
import com.projeto.dto.EstornoCustodiaCreateRequest;
import com.projeto.services.EstornoCustodiaCommand;
import com.projeto.services.CadastroCustodiaService;
import com.projeto.services.TransferenciaCustodiaCommand;
import com.projeto.services.AtribuicaoCustodiaCommand;
import com.projeto.services.ConsultaMovimentacaoCustodiaService;
import com.projeto.resources.exceptions.StandardError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carteiras/{carteiraId}/movimentacoes-custodia")
public class MovimentacaoCustodiaResource {
    private final ConsultaMovimentacaoCustodiaService service;
    private final CadastroCustodiaService cadastro;
    public MovimentacaoCustodiaResource(ConsultaMovimentacaoCustodiaService service,CadastroCustodiaService cadastro){
        this.service=service;this.cadastro=cadastro;
    }
    @PostMapping(consumes="application/json",produces="application/json")
    @Operation(tags="Carteiras",summary="Registrar transferência ou atribuição interna de custódia",
            description="Cadastro discriminado TRANSFERENCIA/ATRIBUICAO. Referências existentes, sem consulta externa. "
                    +"Quantidade decimal exata, BRASIL inteiro e EUA até seis casas. Não altera compras nem cálculos financeiros. "
                    +"Campos financeiros, calculados e de auditoria são proibidos.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required=true,content=@Content(
            schema=@Schema(implementation=CustodiaCadastroSchema.class),examples={
                @io.swagger.v3.oas.annotations.media.ExampleObject(name="transferencia",value="""
                    {"natureza":"TRANSFERENCIA","ticker":"PETR4","mercado":"BRASIL","corretoraOrigemId":1,"corretoraDestinoId":2,"quantidade":"40.000000","dataEfetiva":"2026-10-01"}
                    """),
                @io.swagger.v3.oas.annotations.media.ExampleObject(name="atribuicao",value="""
                    {"natureza":"ATRIBUICAO","ticker":"AAPL","mercado":"EUA","corretoraOrigemId":null,"corretoraDestinoId":2,"quantidade":"0.000001","dataEfetiva":"2026-10-01"}
                    """)
            }))
    @ApiResponses({@ApiResponse(responseCode="201",description="Original persistida; Location aponta para seu detalhe",
                    content=@Content(schema=@Schema(implementation=RegistroCustodiaResponse.class),examples=@io.swagger.v3.oas.annotations.media.ExampleObject(name="ativa",value=CustodiaRestExamples.ATIVA)),
                    headers=@io.swagger.v3.oas.annotations.headers.Header(name="Location",schema=@Schema(type="string",format="uri"))),
            @ApiResponse(responseCode="400",description="REQUEST_INVALIDO ou DATA_EFETIVA_FUTURA",content=@Content(schema=@Schema(implementation=StandardError.class))),
            @ApiResponse(responseCode="404",description="Referência inexistente no contexto",content=@Content(schema=@Schema(implementation=StandardError.class))),
            @ApiResponse(responseCode="409",description="POSICAO_INSUFICIENTE, SALDO_ORIGEM_INSUFICIENTE, POSICAO_CUSTODIA_INDISPONIVEL (details.motivo identifica MOVIMENTACAO_CUSTODIA_INVALIDADA ou CONFLITO_HISTORICO_QUANTITATIVO), LIMITE_POSICAO_CUSTODIA, INTEGRIDADE_DADOS_VIOLADA ou CONFLITO_TRANSACIONAL_TRANSITORIO. Estrutura e referências precedem simulação; sem retry automático.",content=@Content(schema=@Schema(implementation=StandardError.class)))})
    public ResponseEntity<RegistroCustodiaResponse> cadastrar(@PathVariable("carteiraId") Long carteiraId,
            @Valid @RequestBody CustodiaCreateRequest entrada){
        RegistroCustodiaResponse resposta;
        if(entrada instanceof TransferenciaCustodiaCreateRequest){
            resposta=cadastro.transferir(new TransferenciaCustodiaCommand(carteiraId,entrada.getTicker(),entrada.getMercado(),
                    entrada.getCorretoraOrigemId(),entrada.getCorretoraDestinoId(),entrada.getQuantidade(),entrada.getDataEfetiva()));
        }else{
            resposta=cadastro.atribuir(new AtribuicaoCustodiaCommand(carteiraId,entrada.getTicker(),entrada.getMercado(),
                    entrada.getCorretoraDestinoId(),entrada.getQuantidade(),entrada.getDataEfetiva()));
        }
        var location=ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(resposta.id()).toUri();
        return ResponseEntity.created(location).body(resposta);
    }
    @PostMapping(value="/{movimentacaoId}/estornos",produces="application/json")
    @Operation(tags="Carteiras",summary="Estornar uma movimentação interna de custódia",
            description="Neutraliza a original na posição efetiva, sem transferência inversa ou exclusão. "
                    +"Aceita ausência de corpo, {} ou motivo opcional string/null, até 1000 caracteres. Não altera cálculos financeiros.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required=false,content=@Content(mediaType="application/json",
            schema=@Schema(implementation=EstornoCustodiaCreateRequest.class),examples={
                @io.swagger.v3.oas.annotations.media.ExampleObject(name="semMotivo",value="{}"),
                @io.swagger.v3.oas.annotations.media.ExampleObject(name="comMotivo",value="{\"motivo\":\"Correção de custódia\"}")
            }))
    @ApiResponses({@ApiResponse(responseCode="201",description="Original e estorno persistidos; Location do detalhe original",
                    content=@Content(schema=@Schema(implementation=DetalheCustodiaResponse.class),examples=@io.swagger.v3.oas.annotations.media.ExampleObject(name="estornada",value=CustodiaRestExamples.DETALHE_ESTORNADA)),
                    headers=@io.swagger.v3.oas.annotations.headers.Header(name="Location",schema=@Schema(type="string",format="uri"))),
            @ApiResponse(responseCode="400",description="REQUEST_INVALIDO",content=@Content(schema=@Schema(implementation=StandardError.class))),
            @ApiResponse(responseCode="404",description="Carteira ou original inexistente no contexto",content=@Content(schema=@Schema(implementation=StandardError.class))),
            @ApiResponse(responseCode="409",description="ESTORNO_DUPLICADO, POSICAO_INSUFICIENTE, MOVIMENTACAO_CUSTODIA_INVALIDADA, CONFLITO_HISTORICO_QUANTITATIVO ou conflito de integridade/concorrência reconhecido",content=@Content(schema=@Schema(implementation=StandardError.class)))})
    public ResponseEntity<DetalheCustodiaResponse> estornar(@PathVariable("carteiraId") Long carteiraId,
            @PathVariable("movimentacaoId") Long movimentacaoId,@Valid @RequestBody(required=false) EstornoCustodiaCreateRequest entrada){
        var resposta=cadastro.estornar(new EstornoCustodiaCommand(carteiraId,movimentacaoId,entrada==null?null:entrada.getMotivo()));
        var location=ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/carteiras/{carteiraId}/movimentacoes-custodia/{movimentacaoId}")
                .buildAndExpand(carteiraId,resposta.movimentacao().id()).toUri();
        return ResponseEntity.created(location).body(resposta);
    }
    @GetMapping(produces="application/json")
    @Operation(tags="Carteiras",summary="Consultar histórico interno de custódia",
            description="Coleção completa, sem paginação ou truncamento, inclusive posições encerradas. "
                    +"Ordem: dataRegistro DESC, MOVIMENTACAO antes de ESTORNO, id DESC. Não comprova custódia externa. "
                    +"Situação restringe originais; ESTORNO explícito não é excluído pelo filtro de situação.")
    @ApiResponses({@ApiResponse(responseCode="200",description="Histórico completo ou array vazio",content=@Content(
                    array=@io.swagger.v3.oas.annotations.media.ArraySchema(schema=@Schema(implementation=RegistroCustodiaResponse.class)),
                    examples={@io.swagger.v3.oas.annotations.media.ExampleObject(name="vazio",value="[]"),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(name="auditavel",value=CustodiaRestExamples.HISTORICO)})),
            @ApiResponse(responseCode="400",description="REQUEST_INVALIDO",content=@Content(schema=@Schema(implementation=StandardError.class))),
            @ApiResponse(responseCode="404",description="Carteira inexistente",content=@Content(schema=@Schema(implementation=StandardError.class)))})
    public List<RegistroCustodiaResponse> historico(@PathVariable("carteiraId") Long carteiraId,
            @Parameter(schema=@Schema(allowableValues={"TRANSFERENCIA","ATRIBUICAO","ESTORNO"}))
            @RequestParam(name="natureza",required=false) String natureza,
            @Parameter(schema=@Schema(allowableValues={"ATIVA","ESTORNADA"}))
            @RequestParam(name="situacao",required=false) String situacao) {
        return service.historico(carteiraId,natureza,situacao);
    }
    @GetMapping(value="/{movimentacaoId}",produces="application/json")
    @Operation(tags="Carteiras",summary="Consultar detalhe de uma original de custódia",
            description="Identidade contextual da movimentação original; contém estorno confirmado ou null. Não consulta ID de estorno como original.")
    @ApiResponses({@ApiResponse(responseCode="200",description="Original e vínculo no mesmo snapshot",content=@Content(
                    schema=@Schema(implementation=DetalheCustodiaResponse.class),examples={
                        @io.swagger.v3.oas.annotations.media.ExampleObject(name="ativa",value=CustodiaRestExamples.DETALHE_ATIVA),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(name="estornada",value=CustodiaRestExamples.DETALHE_ESTORNADA)})),
            @ApiResponse(responseCode="400",description="REQUEST_INVALIDO",content=@Content(schema=@Schema(implementation=StandardError.class))),
            @ApiResponse(responseCode="404",description="Carteira ou original inexistente no contexto",content=@Content(schema=@Schema(implementation=StandardError.class)))})
    public DetalheCustodiaResponse detalhe(@PathVariable("carteiraId") Long carteiraId,
            @PathVariable("movimentacaoId") Long movimentacaoId) {
        return service.detalhe(carteiraId,movimentacaoId);
    }
}
