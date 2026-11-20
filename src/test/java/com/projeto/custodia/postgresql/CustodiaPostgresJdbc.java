package com.projeto.custodia.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;

/** Bootstrap SQL restrito, separado de Liquibase. Nenhum construtor abre conexões.
 * O transporte injetado é simulado em todos os testes desta etapa. */
public final class CustodiaPostgresJdbc implements Bootstrap {
    @FunctionalInterface interface Transporte { Connection abrir(String url, String usuario, String senha) throws SQLException; }
    private final Transporte transporte;
    private final Set<Connection> abertas = Collections.newSetFromMap(new IdentityHashMap<>());
    CustodiaPostgresJdbc(Transporte transporte) { this.transporte = java.util.Objects.requireNonNull(transporte); }

    /** Opt-in futuro: driver explícito, sem DriverManager, datasource global ou propriedades JVM. */
    public static CustodiaPostgresJdbc driverLocal() {
        return new CustodiaPostgresJdbc((url, usuario, senha) -> {
            var propriedades = new java.util.Properties(); propriedades.setProperty("user", usuario); propriedades.setProperty("password", senha);
            propriedades.setProperty("connectTimeout", "5"); propriedades.setProperty("socketTimeout", "10");
            var c = new org.postgresql.Driver().connect(url, propriedades);
            if (c == null) throw new SQLException("Driver PostgreSQL recusou o destino"); return c;
        });
    }
    public CustodiaPostgresDataSource.Conector conector(CustodiaPostgresManifesto m) {
        return (url, usuario, senha) -> {
            CustodiaPostgresSeguranca.destino(m, url, usuario, senha);
            return transporte.abrir(url, usuario, senha);
        };
    }
    /** Útil ao orquestrador antes de o manifesto existir; o destino continua limitado ao plano gerado. */
    public CustodiaPostgresDataSource.Conector conector(Plano plano) {
        return (url, usuario, senha) -> {
            exigir(plano.url().equals(url) && plano.credenciais().usuario().equals(usuario)
                    && plano.credenciais().senha().equals(senha), "Destino de aplicação não autorizado");
            return transporte.abrir(url, usuario, senha);
        };
    }

    @Override public synchronized void preparar(CustodiaPostgresManifesto m, Administrador admin, Inspecao inspecao) throws SQLException {
        exigir(admin != null && admin.usuario().equals("custodia_boot_" + m.plano().execucao().toString().replace("-", "")), "Administrador de outra execução");
        CustodiaPostgresSeguranca.identidade(m, m.plano().imagem(), inspecao.inspecionar(m));
        Connection c = transporte.abrir(m.plano().url(), admin.usuario(), admin.senha());
        exigir(c != null, "Conexão de bootstrap ausente"); abertas.add(c);
        Exception principal = null;
        try {
            try (var s = c.createStatement(); var rs = s.executeQuery("SELECT current_database(), current_user, current_setting('server_version_num')::integer")) {
                exigir(rs.next() && m.plano().banco().equals(rs.getString(1)) && admin.usuario().equals(rs.getString(2))
                        && rs.getInt(3) == 170011 && !rs.next(), "Sessão de bootstrap divergente");
            }
            try (var s = c.createStatement(); var rs = s.executeQuery("SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace "
                    + "WHERE n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg_toast%' "
                    + "AND n.nspname NOT LIKE 'pg_temp%'")) {
                exigir(rs.next() && rs.getLong(1) == 0, "Bootstrap exige banco vazio");
            }
            c.setAutoCommit(false);
            String usuario = '"' + m.plano().credenciais().usuario() + '"';
            String banco = '"' + m.plano().banco() + '"';
            // Identificadores e segredo são gerados/validados como ASCII fechado; sem SQL fornecido pelo chamador.
            try (var s = c.createStatement()) {
                s.execute("CREATE ROLE " + usuario + " LOGIN PASSWORD '" + m.plano().credenciais().senha()
                        + "' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION");
                s.execute("REVOKE ALL ON DATABASE " + banco + " FROM PUBLIC");
                s.execute("GRANT CONNECT ON DATABASE " + banco + " TO " + usuario);
                s.execute("GRANT USAGE, CREATE ON SCHEMA public TO " + usuario);
                s.execute("CREATE SCHEMA custodia_pg17_guard");
                s.execute("CREATE TABLE custodia_pg17_guard.execucao (execucao varchar(36) PRIMARY KEY, marcador varchar(64) NOT NULL)");
                try (var insert = c.prepareStatement("INSERT INTO custodia_pg17_guard.execucao(execucao,marcador) VALUES (?,?)")) {
                    insert.setString(1, m.plano().execucao().toString()); insert.setString(2, m.plano().credenciais().marcador());
                    exigir(insert.executeUpdate() == 1, "Marcador não registrado");
                }
                s.execute("GRANT USAGE ON SCHEMA custodia_pg17_guard TO " + usuario);
                s.execute("GRANT SELECT ON custodia_pg17_guard.execucao TO " + usuario);
            }
            c.commit();
        } catch (SQLException | RuntimeException falha) {
            principal = falha;
            try { c.rollback(); } catch (SQLException rollback) { falha.addSuppressed(rollback); }
            throw falha;
        } finally {
            // Se close falhar, a referência permanece e a limpeza deve tentar fechar antes de remover recursos.
            try { c.close(); abertas.remove(c); }
            catch (SQLException fechamento) { if (principal != null) principal.addSuppressed(fechamento); else throw fechamento; }
        }
    }
    @Override public synchronized void close() throws SQLException {
        for (var c : new java.util.ArrayList<>(abertas)) { c.close(); abertas.remove(c); }
    }
}
