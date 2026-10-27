package com.projeto.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Null;
import com.projeto.entities.NaturezaMovimentacaoCustodia;

@Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
public final class AtribuicaoCustodiaCreateRequest extends CustodiaCreateRequest {
    @Override public void setNatureza(NaturezaMovimentacaoCustodia valor){
        if(valor!=NaturezaMovimentacaoCustodia.ATRIBUICAO)throw new IllegalArgumentException("Natureza incompatível com atribuição");
        super.setNatureza(valor);
    }
    @Override @Null
    @Schema(nullable=true,description="Exclusivamente omitida ou null; origem é o grupo sem corretora")
    public Long getCorretoraOrigemId(){return super.getCorretoraOrigemId();}
}
