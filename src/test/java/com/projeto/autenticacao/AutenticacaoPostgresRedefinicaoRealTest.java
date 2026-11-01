package com.projeto.autenticacao;

@AutenticacaoPostgresReal
class AutenticacaoPostgresRedefinicaoRealTest extends RedefinicaoConcorrenciaTest {
    @Override void esperarBloqueio() { AutenticacaoPostgresExecucao.esperarBloqueios(dataSource,1); }
}
