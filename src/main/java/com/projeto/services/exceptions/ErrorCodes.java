package com.projeto.services.exceptions;

public final class ErrorCodes {

    public static final String REQUEST_INVALIDO = "REQUEST_INVALIDO";
    public static final String CNPJ_INVALIDO = "CNPJ_INVALIDO";
    public static final String CNPJ_INEXISTENTE = "CNPJ_INEXISTENTE";
    public static final String CEP_INVALIDO = "CEP_INVALIDO";
    public static final String CEP_INEXISTENTE = "CEP_INEXISTENTE";
    public static final String CORRETORA_DUPLICADA = "CORRETORA_DUPLICADA";
    public static final String DADOS_EXTERNOS_INCOMPLETOS = "DADOS_EXTERNOS_INCOMPLETOS";
    public static final String SITUACAO_CADASTRAL_NAO_ATIVA = "SITUACAO_CADASTRAL_NAO_ATIVA";
    public static final String LIMITE_REQUISICOES_EXCEDIDO = "LIMITE_REQUISICOES_EXCEDIDO";
    public static final String SERVICO_EXTERNO_INDISPONIVEL = "SERVICO_EXTERNO_INDISPONIVEL";
    public static final String SERVICO_EXTERNO_TIMEOUT = "SERVICO_EXTERNO_TIMEOUT";
    public static final String RESPOSTA_EXTERNA_INVALIDA = "RESPOSTA_EXTERNA_INVALIDA";

    private ErrorCodes() {
    }
}
