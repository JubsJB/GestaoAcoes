package com.projeto.autenticacao;

@AutenticacaoPostgresReal
class AutenticacaoPostgresSessaoRealTest extends SessaoInatividadeHttpTest {
    @Override void esperarBloqueio() { AutenticacaoPostgresExecucao.esperarBloqueios(dataSource,1); }
}
