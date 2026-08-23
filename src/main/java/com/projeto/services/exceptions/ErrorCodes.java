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
    public static final String TICKER_INVALIDO = "TICKER_INVALIDO";
    public static final String TICKER_INEXISTENTE = "TICKER_INEXISTENTE";
    public static final String ACAO_DUPLICADA = "ACAO_DUPLICADA";
    public static final String COTACAO_INDISPONIVEL = "COTACAO_INDISPONIVEL";
    public static final String COTACAO_FORA_DA_PRECISAO = "COTACAO_FORA_DA_PRECISAO";
    public static final String ORDEM_OPERACAO_DUPLICADA = "ORDEM_OPERACAO_DUPLICADA";
    public static final String POSICAO_INSUFICIENTE = "POSICAO_INSUFICIENTE";
    public static final String CARTEIRA_POSSUI_OPERACOES = "CARTEIRA_POSSUI_OPERACOES";
    public static final String HISTORICO_OPERACOES_INCONSISTENTE = "HISTORICO_OPERACOES_INCONSISTENTE";
    public static final String CALCULO_POSICAO_FORA_DA_PRECISAO = "CALCULO_POSICAO_FORA_DA_PRECISAO";

    private ErrorCodes() {
    }
}
