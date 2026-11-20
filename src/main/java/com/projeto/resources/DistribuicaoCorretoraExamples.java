package com.projeto.resources;

final class DistribuicaoCorretoraExamples {
    private DistribuicaoCorretoraExamples() { }
    static final String EXAMPLE_1 = """
            {"carteiraId":1,"posicoes":[{"acaoId":1,"ticker":"PETR4","mercado":"BRASIL","moeda":"BRL","quantidadeConsolidada":100,"quantidadeAtribuidaTotal":100,"reconciliada":true,"diagnosticoReconciliacao":null,"corretoras":[{"corretoraId":1,"nome":"Corretora A","quantidadeLiquida":80},{"corretoraId":null,"nome":"Sem corretora informada","quantidadeLiquida":20}],"diagnosticosCorretoras":[{"corretoraId":1,"nome":"Corretora A","quantidadeFinal":80,"saldoNegativoAtual":false,"possuiDeficitHistorico":false,"primeiroDeficit":null},{"corretoraId":null,"nome":"Sem corretora informada","quantidadeFinal":20,"saldoNegativoAtual":false,"possuiDeficitHistorico":false,"primeiroDeficit":null}]}]}
            """;
    static final String EXAMPLE_2 = """
            {"carteiraId":1,"posicoes":[{"acaoId":1,"ticker":"PETR4","mercado":"BRASIL","moeda":"BRL","quantidadeConsolidada":60,"quantidadeAtribuidaTotal":60,"reconciliada":true,"diagnosticoReconciliacao":null,"corretoras":[{"corretoraId":1,"nome":"Corretora A","quantidadeLiquida":100},{"corretoraId":2,"nome":"Corretora B","quantidadeLiquida":-40}],"diagnosticosCorretoras":[{"corretoraId":1,"nome":"Corretora A","quantidadeFinal":100,"saldoNegativoAtual":false,"possuiDeficitHistorico":false,"primeiroDeficit":null},{"corretoraId":2,"nome":"Corretora B","quantidadeFinal":-40,"saldoNegativoAtual":true,"possuiDeficitHistorico":true,"primeiroDeficit":{"dominio":"FINANCEIRO","id":20,"natureza":"VENDA","dataEfetiva":"2026-10-02","posicaoCronologica":{"ordemNoDia":2},"saldoAposEvento":-40}}]}]}
            """;
    static final String EXAMPLE_3 = """
            {"carteiraId":1,"posicoes":[{"acaoId":1,"ticker":"PETR4","mercado":"BRASIL","moeda":"BRL","quantidadeConsolidada":110,"quantidadeAtribuidaTotal":110,"reconciliada":true,"diagnosticoReconciliacao":null,"corretoras":[{"corretoraId":1,"nome":"Corretora A","quantidadeLiquida":100},{"corretoraId":2,"nome":"Corretora B","quantidadeLiquida":10}],"diagnosticosCorretoras":[{"corretoraId":1,"nome":"Corretora A","quantidadeFinal":100,"saldoNegativoAtual":false,"possuiDeficitHistorico":false,"primeiroDeficit":null},{"corretoraId":2,"nome":"Corretora B","quantidadeFinal":10,"saldoNegativoAtual":false,"possuiDeficitHistorico":true,"primeiroDeficit":{"dominio":"FINANCEIRO","id":20,"natureza":"VENDA","dataEfetiva":"2026-10-02","posicaoCronologica":{"ordemNoDia":2},"saldoAposEvento":-10}}]}]}
            """;
    static final String EXAMPLE_4 = """
            {"carteiraId":1,"posicoes":[{"acaoId":1,"ticker":"PETR4","mercado":"BRASIL","moeda":"BRL","quantidadeConsolidada":100,"quantidadeAtribuidaTotal":100,"reconciliada":true,"diagnosticoReconciliacao":null,"corretoras":[{"corretoraId":1,"nome":"Corretora A","quantidadeLiquida":100}],"diagnosticosCorretoras":[{"corretoraId":1,"nome":"Corretora A","quantidadeFinal":100,"saldoNegativoAtual":false,"possuiDeficitHistorico":false,"primeiroDeficit":null},{"corretoraId":2,"nome":"Corretora B","quantidadeFinal":0,"saldoNegativoAtual":false,"possuiDeficitHistorico":true,"primeiroDeficit":{"dominio":"FINANCEIRO","id":20,"natureza":"VENDA","dataEfetiva":"2026-10-02","posicaoCronologica":{"ordemNoDia":2},"saldoAposEvento":-10}}]}]}
            """;
    static final String EXAMPLE_5 = """
            {"carteiraId":1,"posicoes":[{"acaoId":1,"ticker":"PETR4","mercado":"BRASIL","moeda":"BRL","quantidadeConsolidada":101,"quantidadeAtribuidaTotal":100,"reconciliada":false,"diagnosticoReconciliacao":{"codigo":"DIVERGENCIA_QUANTIDADES"},"corretoras":[{"corretoraId":1,"nome":"Corretora A","quantidadeLiquida":100}],"diagnosticosCorretoras":[{"corretoraId":1,"nome":"Corretora A","quantidadeFinal":100,"saldoNegativoAtual":false,"possuiDeficitHistorico":false,"primeiroDeficit":null}]}]}
            """;
}
