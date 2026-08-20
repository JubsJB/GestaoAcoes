package com.projeto.integrations.cep;

public record CepData(
        String cep,
        String logradouro,
        String bairro,
        String cidade,
        String uf
) {
}
