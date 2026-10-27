package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.projeto.services.EventoQuantitativo.Dominio;
import com.projeto.services.EventoQuantitativo.Natureza;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(requiredProperties = {"dominio","id","natureza","dataEfetiva","posicaoCronologica","saldoAposEvento"},
        description="Primeiro evento efetivo negativo, não o pior saldo. Exemplo de custódia é estrutural; não autoriza nova escrita deficitária.",
        example="""
            {"dominio":"CUSTODIA","id":12,"natureza":"TRANSFERENCIA","dataEfetiva":"2026-10-01","posicaoCronologica":{"ordemFinanceiraAncora":1,"numerador":"9007199254740993","denominador":"9007199254740994"},"saldoAposEvento":-10.000000}
            """)
public record PrimeiroDeficitResponse(Dominio dominio,Long id,Natureza natureza,LocalDate dataEfetiva,
        @Schema(oneOf={PosicaoFinanceiraDeficitResponse.class,PosicaoCustodiaResponse.class}) Object posicaoCronologica,
        BigDecimal saldoAposEvento) {
}
