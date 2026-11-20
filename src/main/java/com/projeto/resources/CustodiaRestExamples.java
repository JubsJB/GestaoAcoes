package com.projeto.resources;

/** Exemplos documentais de auditoria; não representam custódia institucional comprovada. */
final class CustodiaRestExamples {
    private CustodiaRestExamples() { }
    static final String ATIVA="""
        {"id":10,"tipoRegistro":"MOVIMENTACAO","natureza":"TRANSFERENCIA","situacao":"ATIVA","carteiraId":1,"acaoId":2,"ticker":"PETR4","mercado":"BRASIL","corretoraOrigemId":3,"corretoraDestinoId":4,"quantidade":40.000000,"dataEfetiva":"2026-10-01","dataRegistro":"2026-10-24T12:00:00Z","posicaoCronologica":{"ordemFinanceiraAncora":1,"numerador":"1","denominador":"2"},"movimentacaoOriginalId":null,"estornoId":null,"motivo":null}
        """;
    static final String ESTORNADA="""
        {"id":10,"tipoRegistro":"MOVIMENTACAO","natureza":"TRANSFERENCIA","situacao":"ESTORNADA","carteiraId":1,"acaoId":2,"ticker":"PETR4","mercado":"BRASIL","corretoraOrigemId":3,"corretoraDestinoId":4,"quantidade":40.000000,"dataEfetiva":"2026-10-01","dataRegistro":"2026-10-24T12:00:00Z","posicaoCronologica":{"ordemFinanceiraAncora":1,"numerador":"1","denominador":"2"},"movimentacaoOriginalId":null,"estornoId":20,"motivo":null}
        """;
    static final String ESTORNO="""
        {"id":20,"tipoRegistro":"ESTORNO","natureza":"ESTORNO","situacao":null,"carteiraId":1,"acaoId":2,"ticker":"PETR4","mercado":"BRASIL","corretoraOrigemId":3,"corretoraDestinoId":4,"quantidade":40.000000,"dataEfetiva":"2026-10-01","dataRegistro":"2026-10-24T13:00:00Z","posicaoCronologica":{"ordemFinanceiraAncora":1,"numerador":"1","denominador":"2"},"movimentacaoOriginalId":10,"estornoId":20,"motivo":"Correção"}
        """;
    static final String DETALHE_ATIVA="{\"movimentacao\":"+ATIVA+",\"estorno\":null}";
    static final String DETALHE_ESTORNADA="{\"movimentacao\":"+ESTORNADA+",\"estorno\":"+ESTORNO+"}";
    static final String HISTORICO="["+ESTORNO+","+ESTORNADA+"]";
}
