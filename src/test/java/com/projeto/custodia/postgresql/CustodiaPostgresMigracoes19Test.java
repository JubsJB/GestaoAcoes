package com.projeto.custodia.postgresql;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresMigracoes19.*;

class CustodiaPostgresMigracoes19Test {
    @Test void falhaDeConexaoAntesDasMigrationsIdentificaEtapaSemExporMensagem()throws Exception {
        var ds=org.mockito.Mockito.mock(CustodiaPostgresDataSource.class);
        org.mockito.Mockito.when(ds.getConnection()).thenThrow(new java.sql.SQLException("password=segredo","08006"));
        var erro=assertThrows(CustodiaPostgresHomologacao19.FalhaCaso.class,
                ()->executar(CustodiaPostgresHomologacao19.Cenario.INSTALACAO,ds));
        assertEquals(CustodiaPostgresHomologacao19.CasoFalha.MIGRATIONS,erro.diagnostico.caso());
        assertEquals("08006",erro.diagnostico.sqlState());assertFalse(erro.toString().contains("segredo"));assertNull(erro.getCause());
    }
    @Test void preservacaoIdenticaProduzEvidenciaReproduzivelSemPublicarConteudo()throws Exception{
        var antes=new Retrato(Map.of("operacao",List.of(List.of("401","100.000000"))),Map.of());
        var hash=preservar(antes,antes);assertTrue(hash.matches("[a-f0-9]{64}"));
        assertEquals(hash,preservar(antes,antes));assertThrows(IllegalStateException.class,()->preservar(antes,null));
    }
    @Test void alteracaoDeValorFinanceiroOuEstruturaImpedePreservacao() {
        var antes=new Retrato(Map.of("operacao",List.of(List.of("401","100.000000"))),Map.of("coluna",List.of(List.of("NUMERIC","19","6"))));
        var dadoAlterado=new Retrato(Map.of("operacao",List.of(List.of("401","99.000000"))),antes.estrutura());
        var estruturaAlterada=new Retrato(antes.dados(),Map.of("coluna",List.of(List.of("NUMERIC","18","6"))));
        assertThrows(IllegalStateException.class,()->preservar(antes,dadoAlterado));
        assertThrows(IllegalStateException.class,()->preservar(antes,estruturaAlterada));
    }
    @Test void planoRealTemNoveChangesetsERollbackStopEmAmbosOsChangesets008() throws Exception {
        try (var recursos = new liquibase.resource.ClassLoaderResourceAccessor();
                var lb = new liquibase.Liquibase(MASTER,recursos,new liquibase.database.core.MockDatabase())) {
            assertDoesNotThrow(() -> validarPlano(lb));
            // Perder a proteção de qualquer changeset deve impedir sua execução futura.
            lb.getDatabaseChangeLog().getChangeSets().get(8).getRollback().getChanges().clear();
            assertThrows(IllegalStateException.class,() -> validarPlano(lb));
        }
    }
    static List<Registro> historicoValido() {
        var ids = List.of("001-create-corretora","002-create-acao","003-create-carteira","004-create-operacao",
                "005-create-historico-cotacao","006-create-snapshot-carteira","007-add-referencia-temporal-cotacao",
                "008-create-movimentacoes-custodia","008-add-integridade-movimentacoes-custodia");
        var registros = new ArrayList<Registro>();
        for (int i=0;i<9;i++) registros.add(new Registro(ids.get(i),"codex","db/changelog/changes/"
                +(i==8 ? "008-create-movimentacoes-custodia" : ids.get(i))+".yaml","9:checksum","EXECUTED",i+1));
        return registros;
    }
    @Test void aceitaHistoricosCompletosDaInstalacaoEDaBaseAnterior() {
        assertDoesNotThrow(() -> validarHistorico(historicoValido(),9));
        assertDoesNotThrow(() -> validarHistorico(historicoValido().subList(0,7),7));
    }
    @ParameterizedTest @ValueSource(strings={"faltante","id","autor","arquivo","checksum","tipo","ordem"})
    void historicoParcialOuAlteradoNaoComprovaInstalacao(String defeito) {
        var registros = historicoValido(); var antigo = registros.get(8);
        registros.set(8,new Registro(defeito.equals("id") ? "outro" : antigo.id(),
                defeito.equals("autor") ? "outro" : antigo.autor(),defeito.equals("arquivo") ? "outro" : antigo.arquivo(),
                defeito.equals("checksum") ? null : antigo.checksum(),defeito.equals("tipo") ? "MARK_RAN" : antigo.tipo(),
                defeito.equals("ordem") ? 7 : antigo.ordem()));
        if (defeito.equals("faltante")) registros.remove(8);
        assertThrows(IllegalStateException.class,() -> validarHistorico(registros,9));
    }
}
