package com.projeto.custodia.postgresql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** JDBC inteiramente simulado, sem construir o driver real ou executar SQL em banco. */
class CustodiaPostgresJdbcTest {
    private final Imagem pin = new Imagem(IMAGEM, "sha256:" + "a".repeat(64));
    private final Plano plano = Plano.novo(pin, 55437);
    private final CustodiaPostgresManifesto m = new CustodiaPostgresManifesto(plano, "b".repeat(64), plano.volume(), "c".repeat(64));
    private final Administrador admin = Administrador.novo(plano);
    private final Inspecao inspecao = mock(Inspecao.class);
    private final CustodiaPostgresJdbc.Transporte transporte = mock(CustodiaPostgresJdbc.Transporte.class);

    @Test void bootstrapCriaMarcadorEUsuarioLimitadoDepoisDeVerificarIdentidadeEBancoVazio() throws Exception {
        var f = simular("valida"); var jdbc = new CustodiaPostgresJdbc(transporte);
        jdbc.preparar(m, admin, inspecao);
        var ordem = inOrder(inspecao, transporte, f.c(), f.ddl());
        ordem.verify(inspecao).inspecionar(m);
        ordem.verify(transporte).abrir(plano.url(), admin.usuario(), admin.senha());
        ordem.verify(f.c()).setAutoCommit(false);
        ordem.verify(f.ddl()).execute(contains("NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION"));
        ordem.verify(f.c()).commit(); ordem.verify(f.c()).close();
        verify(f.insert()).setString(1, plano.execucao().toString()); verify(f.insert()).setString(2, plano.credenciais().marcador());
        jdbc.close(); verify(f.c(), times(1)).close();
    }

    @ParameterizedTest @ValueSource(strings = {"banco", "usuario", "versao", "naoVazio"})
    void sessaoErradaOuBancoNaoVazioNaoRecebemDdl(String caso) throws Exception {
        var f = simular(caso); var jdbc = new CustodiaPostgresJdbc(transporte);
        assertThrows(IllegalStateException.class, () -> jdbc.preparar(m, admin, inspecao));
        verify(f.ddl(), never()).execute(anyString()); verify(f.c(), never()).commit(); verify(f.c()).close();
    }

    @Test void inspecaoInvalidaImpedeConexaoAdministrativa() throws Exception {
        when(inspecao.inspecionar(m)).thenThrow(new IllegalStateException("sintética"));
        assertThrows(IllegalStateException.class, () -> new CustodiaPostgresJdbc(transporte).preparar(m, admin, inspecao));
        verifyNoInteractions(transporte);
    }

    @Test void falhaDeDdlCausaRollbackEFechaConexao() throws Exception {
        var f = simular("valida"); doThrow(new SQLException("sintética")).when(f.ddl()).execute(anyString());
        assertThrows(SQLException.class, () -> new CustodiaPostgresJdbc(transporte).preparar(m, admin, inspecao));
        verify(f.c()).rollback(); verify(f.c(), never()).commit(); verify(f.c()).close();
    }

    @Test void falhaDeCloseMantemReferenciaParaLimpezaPosterior() throws Exception {
        var f = simular("valida"); doThrow(new SQLException("close sintético")).when(f.c()).close();
        var jdbc = new CustodiaPostgresJdbc(transporte); assertThrows(SQLException.class, () -> jdbc.preparar(m, admin, inspecao));
        assertThrows(SQLException.class, jdbc::close); verify(f.c(), times(2)).close();
        doNothing().when(f.c()).close(); jdbc.close(); verify(f.c(), times(3)).close();
    }

    @Test void conectorDeAplicacaoValidaDestinoAntesDoTransporte() throws Exception {
        var jdbc = new CustodiaPostgresJdbc(transporte); var conector = jdbc.conector(plano);
        assertThrows(IllegalStateException.class, () -> conector.abrir("jdbc:postgresql://localhost:5432/gestaoacoesdb", plano.credenciais().usuario(), plano.credenciais().senha()));
        assertThrows(IllegalStateException.class, () -> jdbc.conector(m).abrir(plano.url(), "postgres", plano.credenciais().senha()));
        verifyNoInteractions(transporte);
        var c = mock(Connection.class); when(transporte.abrir(plano.url(), plano.credenciais().usuario(), plano.credenciais().senha())).thenReturn(c);
        assertSame(c, conector.abrir(plano.url(), plano.credenciais().usuario(), plano.credenciais().senha()));
    }

    private Fixture simular(String caso) throws SQLException {
        var r = new Recurso(Tipo.CONTAINER, m.containerId(), plano.container(), plano.inicio().plusMillis(1), false, plano.rotulos());
        var v = new Recurso(Tipo.VOLUME, m.volumeId(), plano.volume(), plano.inicio().plusMillis(1), false, plano.rotulos());
        var n = new Recurso(Tipo.REDE, m.redeId(), plano.rede(), plano.inicio().plusMillis(1), false, plano.rotulos());
        when(inspecao.inspecionar(m)).thenReturn(new Estado(r, v, n, pin, "127.0.0.1", plano.porta(), plano.banco(), plano.credenciais(), true,
                plano.volume(), "/var/lib/postgresql/data", java.util.Set.of(m.redeId()), false, false, false));
        var c = mock(Connection.class); when(transporte.abrir(plano.url(), admin.usuario(), admin.senha())).thenReturn(c);
        var primeiro = mock(Statement.class); var segundo = mock(Statement.class); var ddl = mock(Statement.class);
        var identidade = mock(ResultSet.class); when(identidade.next()).thenReturn(true, false);
        when(identidade.getString(1)).thenReturn(caso.equals("banco") ? "gestaoacoesdb" : plano.banco());
        when(identidade.getString(2)).thenReturn(caso.equals("usuario") ? "postgres" : admin.usuario());
        when(identidade.getInt(3)).thenReturn(caso.equals("versao") ? 160008 : 170011);
        when(primeiro.executeQuery(anyString())).thenReturn(identidade);
        var vazio = mock(ResultSet.class); when(vazio.next()).thenReturn(true); when(vazio.getLong(1)).thenReturn(caso.equals("naoVazio") ? 1L : 0L);
        when(segundo.executeQuery(anyString())).thenReturn(vazio); when(c.createStatement()).thenReturn(primeiro, segundo, ddl);
        var insert = mock(PreparedStatement.class); when(c.prepareStatement(anyString())).thenReturn(insert); when(insert.executeUpdate()).thenReturn(1);
        return new Fixture(c, ddl, insert);
    }
    private record Fixture(Connection c, Statement ddl, PreparedStatement insert) { }
}
