package com.projeto.custodia.postgresql;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CustodiaPostgresExperimentoCapacidadeTest {
    @Test void crescimentoAlternadoAtingeLimiteExplicitoSemAlterarVizinhas() {
        assertEquals(2449, CustodiaPostgresExperimentoCapacidade.crescimentoAdversarial());
    }

    @Test void roteiroJdbcConfereTodasAsAmostrasEReverteEstruturas() throws Exception {
        var jdbc = new JdbcSimulado();
        var r = CustodiaPostgresExperimentoCapacidade.executar(jdbc.c);
        assertEquals(2467, r.roundtrips()); assertEquals(2449, r.adversariais());
        assertEquals(65536, r.bytesIndice());
        verify(jdbc.c).rollback(); verify(jdbc.c, never()).commit();
        assertEquals(8, CustodiaPostgresExperimentoCapacidade.limites().size());
        for (var l : CustodiaPostgresExperimentoCapacidade.limites()) {
            assertEquals(512,l.n().length()); assertEquals(512,l.d().length());
        }
    }

    @ParameterizedTest @ValueSource(strings={"metadados","roundtrip","roundtripAlterado","ordem","contagem","tamanho","plano"})
    void resultadoJdbcIncompletoInterrompeEReverte(String etapa) throws Exception {
        var jdbc = new JdbcSimulado(); jdbc.defeito = etapa;
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresExperimentoCapacidade.executar(jdbc.c));
        verify(jdbc.c).rollback(); verify(jdbc.c,never()).commit();
    }

    @Test void erroSqlInesperadoInterrompeSemSerConfundidoComDuplicidadeEsperada() throws Exception {
        var jdbc = new JdbcSimulado(); jdbc.defeito = "sqlstate";
        var erro = assertThrows(SQLException.class, () -> CustodiaPostgresExperimentoCapacidade.executar(jdbc.c));
        assertEquals("54000",erro.getSQLState()); verify(jdbc.c).rollback();
    }

    /** Respostas JDBC sintéticas. Não implementa nem certifica semântica do PostgreSQL. */
    static final class JdbcSimulado {
        final Connection c = mock(Connection.class);
        final List<List<Object>> linhas = new ArrayList<>();
        String defeito = "";
        JdbcSimulado() throws Exception {
            when(c.setSavepoint()).thenAnswer(i -> mock(Savepoint.class));
            when(c.createStatement()).thenAnswer(i -> {
                var s = mock(Statement.class);
                when(s.executeQuery(anyString())).thenAnswer(q -> {
                    String sql = q.getArgument(0);
                    if (sql.contains("server_version_num")) return rs(List.of(List.of(170011,8192)),"metadados");
                    if (sql.contains("ORDER BY")) return rs(List.of(List.of("10"),List.of("2")),"ordem");
                    if (sql.contains("count(*)")) return rs(List.of(List.of((long)linhas.size())),"contagem");
                    if (sql.contains("pg_relation_size")) return rs(List.of(List.of(65536L)),"tamanho");
                    throw new SQLException("Consulta inesperada no roteiro simulado");
                }); return s;
            });
            when(c.prepareStatement(anyString())).thenAnswer(i -> {
                String sql = i.getArgument(0); var p = mock(PreparedStatement.class);
                var parametros = new TreeMap<Integer,Object>();
                doAnswer(a -> { parametros.put(a.getArgument(0),a.getArgument(1)); return null; }).when(p).setLong(anyInt(),anyLong());
                doAnswer(a -> { parametros.put(a.getArgument(0),a.getArgument(1)); return null; }).when(p).setInt(anyInt(),anyInt());
                doAnswer(a -> { parametros.put(a.getArgument(0),a.getArgument(1)); return null; }).when(p).setString(anyInt(),anyString());
                doAnswer(a -> { parametros.put(a.getArgument(0),a.getArgument(1)); return null; }).when(p).setObject(anyInt(),any());
                when(p.executeUpdate()).thenAnswer(a -> {
                    var valores = List.copyOf(parametros.values());
                    String n = (String)valores.get(4);
                    if (n.length()==513) throw new SQLException("sintética","22001");
                    if (n.equals("01") || n.equals("1\n")) throw new SQLException("sintética","23514");
                    if (linhas.contains(valores)) throw new SQLException("sintética",defeito.equals("sqlstate") ? "54000" : "23505");
                    linhas.add(valores); return 1;
                });
                when(p.executeQuery()).thenAnswer(a -> {
                    if (sql.startsWith("EXPLAIN")) return rs(List.of(List.of("[{\"Plan\":{\"Total Cost\":1.2}}]")),"plano");
                    var valores = List.copyOf(parametros.values());
                    if (!linhas.contains(valores)) return rs(List.of(),"roundtrip");
                    return rs(List.of(List.of(valores.get(4),valores.get(5))),"roundtrip");
                }); return p;
            });
        }
        private ResultSet rs(List<? extends List<?>> dados, String etapa) throws SQLException {
            var rs = mock(ResultSet.class); int[] cursor = {-1};
            when(rs.next()).thenAnswer(a -> ++cursor[0] < (defeito.equals(etapa) ? 0 : dados.size()));
            when(rs.getString(anyInt())).thenAnswer(a -> etapa.equals("roundtrip") && defeito.equals("roundtripAlterado")
                    ? "9" : dados.get(cursor[0]).get((int)a.getArgument(0)-1).toString());
            when(rs.getInt(anyInt())).thenAnswer(a -> ((Number)dados.get(cursor[0]).get((int)a.getArgument(0)-1)).intValue());
            when(rs.getLong(anyInt())).thenAnswer(a -> ((Number)dados.get(cursor[0]).get((int)a.getArgument(0)-1)).longValue());
            return rs;
        }
    }
}
