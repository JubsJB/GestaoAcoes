package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.projeto.entities.Mercado;
import com.projeto.entities.NaturezaMovimentacaoCustodia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME,include=JsonTypeInfo.As.EXISTING_PROPERTY,property="natureza",visible=true)
@JsonSubTypes({@JsonSubTypes.Type(value=TransferenciaCustodiaCreateRequest.class,name="TRANSFERENCIA"),
        @JsonSubTypes.Type(value=AtribuicaoCustodiaCreateRequest.class,name="ATRIBUICAO")})
@Schema(oneOf={TransferenciaCustodiaCreateRequest.class,AtribuicaoCustodiaCreateRequest.class},
        discriminatorProperty="natureza",additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
public abstract class CustodiaCreateRequest {
    @NotNull private NaturezaMovimentacaoCustodia natureza;
    @NotBlank private String ticker;
    @NotNull private Mercado mercado;
    private Long corretoraOrigemId;
    @NotNull @Positive private Long corretoraDestinoId;
    @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=13,fraction=6)
    @Schema(types={"number","string"},description="Número JSON exato ou string decimal vinculados diretamente a BigDecimal; positivo, NUMERIC(19,6), BRASIL inteiro e EUA até seis casas, sem arredondamento")
    private BigDecimal quantidade;
    @NotNull private LocalDate dataEfetiva;

    public NaturezaMovimentacaoCustodia getNatureza(){return natureza;}
    public void setNatureza(NaturezaMovimentacaoCustodia valor){natureza=valor;}
    public String getTicker(){return ticker;}
    public void setTicker(String valor){ticker=valor;}
    public Mercado getMercado(){return mercado;}
    public void setMercado(Mercado valor){mercado=valor;}
    public Long getCorretoraOrigemId(){return corretoraOrigemId;}
    public void setCorretoraOrigemId(Long valor){corretoraOrigemId=valor;}
    public Long getCorretoraDestinoId(){return corretoraDestinoId;}
    public void setCorretoraDestinoId(Long valor){corretoraDestinoId=valor;}
    public BigDecimal getQuantidade(){return quantidade;}
    public void setQuantidade(BigDecimal valor){quantidade=valor;}
    public LocalDate getDataEfetiva(){return dataEfetiva;}
    public void setDataEfetiva(LocalDate valor){dataEfetiva=valor;}
    @JsonAnySetter
    public final void rejectUnknownProperty(String campo,Object valor){
        throw new IllegalArgumentException("Campo não permitido no cadastro de custódia: "+campo);
    }
}
