package com.projeto.integrations.cep;

public interface CepProvider {

    CepData consultar(String cepNormalizado);
}
