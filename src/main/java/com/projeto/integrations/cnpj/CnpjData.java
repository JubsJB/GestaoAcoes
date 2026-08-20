package com.projeto.integrations.cnpj;

public record CnpjData(
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String email,
        String telefone,
        String cep,
        String numero,
        String complemento,
        String situacaoCadastral
) {
}
