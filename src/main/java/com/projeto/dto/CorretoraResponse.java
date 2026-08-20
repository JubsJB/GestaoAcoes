package com.projeto.dto;

import java.time.OffsetDateTime;

/**
 * Representa a Corretora persistida. Enquanto nao houver uma fonte publica aprovada,
 * {@code validadaMercadoFinanceiro=false} significa somente que a validacao ainda nao foi realizada.
 */
public record CorretoraResponse(
        Long id,
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String email,
        String telefone,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String situacaoCadastral,
        boolean validadaMercadoFinanceiro,
        OffsetDateTime dataCadastro
) {
}
