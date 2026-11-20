package com.projeto.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.projeto.entities.NaturezaMovimentacaoCustodia;

@Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
public final class TransferenciaCustodiaCreateRequest extends CustodiaCreateRequest {
    @Override public void setNatureza(NaturezaMovimentacaoCustodia valor){
        if(valor!=NaturezaMovimentacaoCustodia.TRANSFERENCIA)throw new IllegalArgumentException("Natureza incompatível com transferência");
        super.setNatureza(valor);
    }
    @Override @NotNull @Positive
    public Long getCorretoraOrigemId(){return super.getCorretoraOrigemId();}
}
