package com.projeto.autenticacao;

@AutenticacaoPostgresReal
class AutenticacaoPostgresAtomicidadeRealTest extends RedefinicaoAtomicidadeTest {
    private String tabela;
    @Override void falhar(String tabela,String evento) {
        this.tabela=tabela;AutenticacaoPostgresExecucao.instalarFalha(dataSource,tabela,evento,"falha_redefinicao");
    }
    @Override void removerFalha() { AutenticacaoPostgresExecucao.removerFalha(dataSource,tabela,"falha_redefinicao"); }
}
