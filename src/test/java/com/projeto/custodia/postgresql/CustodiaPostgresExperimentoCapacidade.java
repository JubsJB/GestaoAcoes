package com.projeto.custodia.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.exigir;
import static com.projeto.custodia.postgresql.CustodiaPostgresCapacidade.*;
import com.projeto.entities.PosicaoRacionalCustodia;
import com.projeto.entities.LimitePosicaoCustodiaException;

/** SQL experimental de 1.10, sem migrations ou tabelas da aplicação. */
final class CustodiaPostgresExperimentoCapacidade {
    @FunctionalInterface interface Amostra { void aceitar(PosicaoRacionalCustodia p) throws Exception; }
    static int crescimentoAdversarial() {
        try { return crescimento(p -> { }); }
        catch (Exception e) { throw new IllegalStateException("Crescimento incompleto"); }
    }
    static int crescimento(Amostra amostra) throws Exception {
        var esquerda = PosicaoRacionalCustodia.INICIO;
        var direita = PosicaoRacionalCustodia.FIM;
        for (int i = 0; i < 3000; i++) {
            String a = esquerda.toString(), b = direita.toString();
            PosicaoRacionalCustodia nova;
            try { nova = esquerda.entre(direita); }
            catch (LimitePosicaoCustodiaException limite) {
                if (i != 2449 || !a.equals(esquerda.toString()) || !b.equals(direita.toString()))
                    throw new IllegalStateException("Limite adversarial divergente");
                return i;
            }
            if (nova.compareTo(esquerda) <= 0 || nova.compareTo(direita) >= 0)
                throw new IllegalStateException("Medianta fora do intervalo");
            amostra.aceitar(nova);
            if ((i & 1) == 0) esquerda = nova; else direita = nova;
        }
        throw new IllegalStateException("Limite adversarial não atingido");
    }
    static final String TABELA = "custodia_capacidade_110";
    static final String INDICE = "custodia_capacidade_chave";
    static final String COLUNAS = "carteira_id,acao_id,data_efetiva,ordem_financeira_ancora,posicao_numerador,posicao_denominador";
    static final String DDL = "CREATE TABLE " + TABELA + " (carteira_id bigint NOT NULL, acao_id bigint NOT NULL, "
            + "data_efetiva date NOT NULL, ordem_financeira_ancora integer NOT NULL, "
            + "posicao_numerador varchar(512) NOT NULL CHECK (posicao_numerador ~ '^[1-9][0-9]*$' AND posicao_numerador !~ '[^0-9]'), "
            + "posicao_denominador varchar(512) NOT NULL CHECK (posicao_denominador ~ '^[1-9][0-9]*$' AND posicao_denominador !~ '[^0-9]'))";
    record Linha(long carteira, long acao, LocalDate dia, int ancora, String n, String d) { }
    static List<Linha> limites() {
        var random = new Random(11017); var linhas = new ArrayList<Linha>();
        for (int i = 0; i < 8; i++) {
            var texto = new StringBuilder("1");
            for (int j = 1; j < 512; j++) texto.append(random.nextInt(10));
            var n = new BigInteger(texto.toString());
            linhas.add(linha(1, n.toString(), n.add(BigInteger.ONE).toString()));
        }
        return List.copyOf(linhas);
    }
    private static Linha linha(int ancora, String n, String d) {
        return new Linha(1, 1, LocalDate.of(2001, 2, 3), ancora, n, d);
    }
    static Resultado executar(Connection c) throws Exception {
        // O datasource do executor já comprovou banco, usuário, versão e marcador antes deste SQL.
        c.setAutoCommit(false);
        try {
            int versao, bloco;
            try (var s = c.createStatement(); var rs = s.executeQuery(
                    "SELECT current_setting('server_version_num')::integer,current_setting('block_size')::integer")) {
                exigir(rs.next(), "Metadados ausentes"); versao = rs.getInt(1); bloco = rs.getInt(2);
                exigir(versao == 170011 && bloco > 0 && !rs.next(), "Metadados divergentes");
            }
            try (var s = c.createStatement()) {
                s.execute(DDL);
                s.execute("CREATE UNIQUE INDEX " + INDICE + " ON " + TABELA + " (" + COLUNAS + ")");
            }
            int roundtrips = 0;
            var limites = limites();
            for (var l : limites) { inserirConferir(c, l, true); roundtrips++; }
            rejeitar(c, limites.get(0), "23505");
            // Cada componente participa efetivamente da unicidade composta.
            var a = limites.get(0);
            var distintas = List.of(new Linha(2,1,a.dia(),1,a.n(),a.d()), new Linha(1,2,a.dia(),1,a.n(),a.d()),
                    new Linha(1,1,a.dia().plusDays(1),1,a.n(),a.d()), new Linha(1,1,a.dia(),2,a.n(),a.d()),
                    linha(1,"1",a.d()), linha(1,a.n(),new BigInteger(a.d()).add(BigInteger.ONE).toString()));
            for (var l : distintas) { inserirConferir(c, l, false); roundtrips++; }
            // Ordem textual 10 < 2; ordem racional 10/11 > 2/3.
            var dez = linha(3,"10","11"); var dois = linha(3,"2","3");
            inserirConferir(c, dez, true); inserirConferir(c, dois, true); roundtrips += 2;
            exigir(PosicaoRacionalCustodia.deTexto(dez.n(),dez.d()).compareTo(
                    PosicaoRacionalCustodia.deTexto(dois.n(),dois.d())) > 0, "Comparação matemática divergente");
            try (var s = c.createStatement(); var rs = s.executeQuery("SELECT posicao_numerador FROM " + TABELA
                    + " WHERE ordem_financeira_ancora=3 ORDER BY posicao_numerador COLLATE \"C\"")) {
                exigir(rs.next() && "10".equals(rs.getString(1)) && rs.next() && "2".equals(rs.getString(1))
                        && !rs.next(), "Ordenação textual experimental divergente");
            }
            inserirConferir(c, linha(4,"1","2"), true); inserirConferir(c, linha(4,"2","4"), false); roundtrips += 2;
            boolean rejeitou = false;
            try { PosicaoRacionalCustodia.reconstruirPersistida("2","4"); }
            catch (IllegalArgumentException esperado) { rejeitou = true; }
            exigir(rejeitou && PosicaoRacionalCustodia.deTexto("2","4").equals(
                    PosicaoRacionalCustodia.deTexto("1","2")), "Equivalência não verificada");
            rejeitar(c, linha(5,"1".repeat(513),"2"), "22001");
            rejeitar(c, linha(5,"01","2"), "23514");
            rejeitar(c, linha(5,"1\n","2"), "23514");
            int adversariais = crescimento(p -> inserirConferir(c, linha(6,p.getNumeradorTexto(),p.getDenominadorTexto()),true));
            roundtrips += adversariais;
            try (var s = c.createStatement(); var rs = s.executeQuery("SELECT count(*) FROM " + TABELA)) {
                exigir(rs.next() && rs.getLong(1) == roundtrips && !rs.next(), "Contagem incompleta");
            }
            long bytes;
            try (var s = c.createStatement(); var rs = s.executeQuery("SELECT pg_relation_size('" + INDICE + "'::regclass)")) {
                exigir(rs.next(), "Tamanho ausente"); bytes = rs.getLong(1); exigir(bytes > 0 && !rs.next(), "Tamanho inválido");
            }
            // Plano de igualdade na chave completa. Não impõe custo/latência nem altera o planejador.
            try (var s = c.prepareStatement("EXPLAIN (FORMAT JSON) SELECT * FROM " + TABELA + " WHERE " + filtro())) {
                vincular(s,a);
                try (var rs = s.executeQuery()) {
                    exigir(rs.next(), "Plano ausente");
                    var plano = new ObjectMapper().readTree(rs.getString(1));
                    exigir(plano.isArray() && plano.size()==1 && plano.get(0).path("Plan").path("Total Cost").isNumber()
                            && !rs.next(), "Plano incompleto");
                }
            }
            var resultado = new Resultado(versao,bloco,bytes,roundtrips,adversariais,Set.of(Prova.values()));
            resultado.validar(); return resultado;
        } finally {
            // Nenhuma estrutura experimental é commitada; descarte físico pertence ao orquestrador.
            c.rollback();
        }
    }
    private static String filtro() {
        return "carteira_id=? AND acao_id=? AND data_efetiva=? AND ordem_financeira_ancora=? "
                + "AND posicao_numerador=? AND posicao_denominador=?";
    }
    private static void vincular(java.sql.PreparedStatement s, Linha l) throws SQLException {
        s.setLong(1,l.carteira()); s.setLong(2,l.acao()); s.setObject(3,l.dia()); s.setInt(4,l.ancora());
        s.setString(5,l.n()); s.setString(6,l.d());
    }
    private static void inserir(Connection c, Linha l) throws SQLException {
        try (var s = c.prepareStatement("INSERT INTO " + TABELA + " (" + COLUNAS + ") VALUES (?,?,?,?,?,?)")) {
            vincular(s,l); exigir(s.executeUpdate()==1,"Inserção incompleta");
        }
    }
    private static void inserirConferir(Connection c, Linha l, boolean canonica) throws SQLException {
        inserir(c,l);
        try (var s = c.prepareStatement("SELECT posicao_numerador,posicao_denominador FROM " + TABELA + " WHERE " + filtro())) {
            vincular(s,l);
            try (var rs = s.executeQuery()) {
                exigir(rs.next() && l.n().equals(rs.getString(1)) && l.d().equals(rs.getString(2)) && !rs.next(), "Roundtrip inexato");
                if (canonica) {
                    var p = PosicaoRacionalCustodia.reconstruirPersistida(l.n(),l.d());
                    exigir(p.getNumeradorTexto().equals(l.n()) && p.getDenominadorTexto().equals(l.d()), "Reconstrução inexata");
                }
            }
        }
    }
    private static void rejeitar(Connection c, Linha l, String estado) throws SQLException {
        var ponto = c.setSavepoint(); boolean rejeitou = false;
        try { inserir(c,l); }
        catch (SQLException erro) {
            if (!estado.equals(erro.getSQLState())) throw erro;
            rejeitou = true;
        } finally { c.rollback(ponto); c.releaseSavepoint(ponto); }
        exigir(rejeitou,"Banco aceitou entrada inválida");
    }
    private CustodiaPostgresExperimentoCapacidade() { }
}
