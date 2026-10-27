package com.projeto.resources;

import com.projeto.entities.Mercado;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Somente documentação: variantes planas evitam recursão base.oneOf -> subtipo.allOf -> base. */
@Schema(oneOf={CustodiaCadastroSchema.Transferencia.class,CustodiaCadastroSchema.Atribuicao.class},
        discriminatorProperty="natureza",discriminatorMapping={
            @DiscriminatorMapping(value="TRANSFERENCIA",schema=CustodiaCadastroSchema.Transferencia.class),
            @DiscriminatorMapping(value="ATRIBUICAO",schema=CustodiaCadastroSchema.Atribuicao.class)})
public final class CustodiaCadastroSchema {
    private CustodiaCadastroSchema() { }
    @Schema(name="CadastroTransferenciaCustodia",additionalProperties=Schema.AdditionalPropertiesValue.FALSE,
            requiredProperties={"natureza","ticker","mercado","corretoraOrigemId","corretoraDestinoId","quantidade","dataEfetiva"})
    public record Transferencia(@Schema(allowableValues="TRANSFERENCIA") String natureza,String ticker,Mercado mercado,
            @Schema(minimum="1") Long corretoraOrigemId,@Schema(minimum="1") Long corretoraDestinoId,
            @Schema(types={"number","string"},description="Decimal positivo NUMERIC(19,6), BRASIL inteiro e EUA até seis casas; BigDecimal sem arredondamento") BigDecimal quantidade,
            LocalDate dataEfetiva) { }
    @Schema(name="CadastroAtribuicaoCustodia",additionalProperties=Schema.AdditionalPropertiesValue.FALSE,
            requiredProperties={"natureza","ticker","mercado","corretoraDestinoId","quantidade","dataEfetiva"})
    public record Atribuicao(@Schema(allowableValues="ATRIBUICAO") String natureza,String ticker,Mercado mercado,
            @Schema(types={"null"},nullable=true,description="Omitida ou null") Long corretoraOrigemId,@Schema(minimum="1") Long corretoraDestinoId,
            @Schema(types={"number","string"},description="Decimal positivo NUMERIC(19,6), BRASIL inteiro e EUA até seis casas; BigDecimal sem arredondamento") BigDecimal quantidade,
            LocalDate dataEfetiva) { }
}
