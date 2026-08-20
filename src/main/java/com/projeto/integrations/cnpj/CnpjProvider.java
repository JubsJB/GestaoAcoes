package com.projeto.integrations.cnpj;

public interface CnpjProvider {

    CnpjData consultar(String cnpjNormalizado);
}
