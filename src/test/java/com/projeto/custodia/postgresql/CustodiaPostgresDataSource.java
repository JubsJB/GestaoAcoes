package com.projeto.custodia.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.datasource.AbstractDataSource;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;

/** Sem URL configurável, pool, DriverManager ou conector real padrão. */
public final class CustodiaPostgresDataSource extends AbstractDataSource implements AutoCloseable {
    @FunctionalInterface public interface Conector {
        Connection abrir(String url, String usuario, String senha) throws SQLException;
    }
    private final CustodiaPostgresManifesto manifesto;
    private final Imagem pin;
    private final Inspecao inspecao;
    private final ConfigurableEnvironment ambiente;
    private final Conector conector;
    private final Set<Connection> abertas = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean encerrado;

    public CustodiaPostgresDataSource(CustodiaPostgresManifesto manifesto, Imagem pin,
            Inspecao inspecao, ConfigurableEnvironment ambiente, Conector conector) {
        exigir(manifesto != null && pin != null && inspecao != null && ambiente != null && conector != null,
                "Dependências explícitas obrigatórias");
        CustodiaPostgresSeguranca.ambiente(ambiente);
        exigir(pin.equals(manifesto.plano().imagem()), "Pin diferente do aprovado");
        this.manifesto = manifesto; this.pin = pin; this.inspecao = inspecao;
        this.ambiente = ambiente; this.conector = conector;
    }

    @Override public synchronized Connection getConnection() throws SQLException {
        exigir(!encerrado, "Datasource encerrado");
        CustodiaPostgresSeguranca.ambiente(ambiente);
        CustodiaPostgresSeguranca.identidade(manifesto, pin, inspecao.inspecionar(manifesto));
        var p = manifesto.plano();
        CustodiaPostgresSeguranca.destino(manifesto, p.url(), p.credenciais().usuario(), p.credenciais().senha());
        Connection c = conector.abrir(p.url(), p.credenciais().usuario(), p.credenciais().senha());
        exigir(c != null, "Conector não forneceu conexão");
        abertas.add(c);
        try {
            CustodiaPostgresSeguranca.sessao(c, manifesto);
            return c;
        } catch (SQLException | RuntimeException falha) {
            try { c.close(); abertas.remove(c); } catch (SQLException fechamento) { falha.addSuppressed(fechamento); }
            throw falha;
        }
    }

    @Override public Connection getConnection(String usuario, String senha) throws SQLException {
        throw new SQLException("Credenciais externas não são aceitas");
    }

    /** Guarda também referências já fechadas pelo consumidor; close JDBC deve ser idempotente.
     * Nenhuma remoção é liberada se alguma conexão não puder ser fechada. */
    @Override public synchronized void close() throws SQLException {
        encerrado = true;
        SQLException falha = null;
        for (Connection c : new java.util.ArrayList<>(abertas)) {
            try { c.close(); abertas.remove(c); }
            catch (SQLException e) { if (falha == null) falha = e; else falha.addSuppressed(e); }
        }
        if (falha != null) throw falha;
    }
    public synchronized boolean encerradoSemConexoes() { return encerrado && abertas.isEmpty(); }
    CustodiaPostgresManifesto manifesto() { return manifesto; }
}
