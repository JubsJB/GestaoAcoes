package com.projeto.custodia.postgresql;

import java.util.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;

/** Somente tarefa 1.9. Provisionamento e descarte pertencem à Execucao existente. */
final class CustodiaPostgresHomologacao19 {
    enum Cenario { INSTALACAO, UPGRADE }
    enum CasoFalha { PREPARACAO, ROTEIRO, MIGRATIONS, SCHEMA, IDEMPOTENCIA, PERSISTENCIA,
        INTEGRIDADE_INDICE, PRESERVACAO, ROLLBACK, LIMPEZA, CONCLUIDO, REJEICAO_SQL,
        NATUREZA_INVALIDA, QUANTIDADE_ZERO, QUANTIDADE_NEGATIVA, ANCORA, SEM_ORIGEM, MESMA_CORRETORA, ATRIBUICAO_COM_ORIGEM }
    enum Categoria { NENHUMA, SQL, ASSERTIVA, EXECUCAO, SQL_AUSENTE, SQLSTATE_DIVERGENTE, IDENTIFICADOR_DIVERGENTE }
    record Diagnostico(CasoFalha caso, Categoria categoria, String sqlState, String constraint) { }
    /** Transporta somente valores controlados, nunca a exceção externa. */
    static final class FalhaCaso extends AssertionError {
        final Diagnostico diagnostico;
        FalhaCaso(Diagnostico d) { super("Verificação 1.9 interrompida: "+d); diagnostico=d; }
    }
    static Diagnostico diagnosticar(CasoFalha caso, Throwable falha) {
        if(falha instanceof FalhaCaso segura) return segura.diagnostico;
        var vistos=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
        for(Throwable atual=falha;atual!=null && vistos.size()<16 && vistos.add(atual);atual=atual.getCause()) {
            if(atual instanceof java.sql.SQLException sql)
                return new Diagnostico(caso,Categoria.SQL,estadoSeguro(sql),constraintSegura(sql));
        }
        return new Diagnostico(caso,falha instanceof AssertionError ? Categoria.ASSERTIVA : Categoria.EXECUCAO,"NA","NA");
    }
    static String estadoSeguro(java.sql.SQLException sql) {
        String estado=sql.getSQLState();return estado!=null && estado.matches("[A-Z0-9]{5}") ? estado : "NA";
    }
    static String constraintSegura(java.sql.SQLException sql) {
        var detalhes=sql instanceof org.postgresql.util.PSQLException pg ? pg.getServerErrorMessage() : null;
        String nome=detalhes==null ? null : detalhes.getConstraint();
        return nome!=null && CustodiaPostgresIntegridade19.CONSTRAINTS.contains(nome) ? nome : "NA";
    }
    static FalhaCaso falha(CasoFalha caso,Throwable erro) { return new FalhaCaso(diagnosticar(caso,erro)); }
    enum Prova { MIGRATIONS, LEGADO, SCHEMA, HIBERNATE, PERSISTENCIA, INTEGRIDADE, INDICE, REPOSITORIOS, REPLAY, IDEMPOTENCIA, ROLLBACK }
    record Resultado(Set<Prova> provas, int changeSets, long bytesIndice) {
        Resultado { provas = Set.copyOf(provas); }
        void validar(Cenario cenario) {
            var obrigatorias = EnumSet.allOf(Prova.class);
            if (cenario == Cenario.INSTALACAO) obrigatorias.remove(Prova.LEGADO);
            exigir(changeSets==9 && bytesIndice>0 && provas.containsAll(obrigatorias),"Evidências incompletas de 1.9");
        }
    }
    record Relatorio(UUID execucao, Cenario cenario, boolean completo, boolean limpo,
            Map<Tipo,String> pendentes, int incertos, String sqlState, Resultado resultado, Diagnostico diagnostico) { }
    static final class Interrompida extends IllegalStateException {
        final Relatorio relatorio;
        Interrompida(Relatorio r) { super("Homologação 1.9 não aprovada: " + r); relatorio = r; }
    }
    @FunctionalInterface interface Roteiro { Resultado executar(Cenario cenario, CustodiaPostgresDataSource ds) throws Exception; }
    static Relatorio executar(Execucao e, Imagem pin, Cenario cenario, Roteiro roteiro) {
        exigir(e != null && pin != null && cenario != null && roteiro != null,"Dependências explícitas obrigatórias");
        Resultado resultado = null; boolean completo = false, limpo = false;
        CasoFalha caso=CasoFalha.PREPARACAO;
        Diagnostico diagnostico=new Diagnostico(caso,Categoria.NENHUMA,"NA","NA");
        try {
            exigir(pin.equals(e.plano().imagem()),"Pin independente divergente");
            e.provisionar(); var m = e.manifesto();
            exigir(m != null && e.plano().equals(m.plano()),"Manifesto de outra execução");
            CustodiaPostgresSeguranca.identidade(m,pin,e.inspecionar(m));
            caso=CasoFalha.ROTEIRO;resultado = roteiro.executar(cenario,e.datasource());
            exigir(resultado!=null,"Resultado ausente"); resultado.validar(cenario); completo = true;
            diagnostico=new Diagnostico(CasoFalha.CONCLUIDO,Categoria.NENHUMA,"NA","NA");
        } catch (Exception | AssertionError falha) {
            diagnostico=diagnosticar(caso,falha);
        } finally {
            try { e.limpar(); limpo = e.recibos().isEmpty() && e.pendenciasSemRecibo().isEmpty(); }
            catch (Exception falha) { if(completo) diagnostico=diagnosticar(CasoFalha.LIMPEZA,falha); }
        }
        if(completo && !limpo && diagnostico.categoria()==Categoria.NENHUMA)
            diagnostico=new Diagnostico(CasoFalha.LIMPEZA,Categoria.EXECUCAO,"NA","NA");
        var recibos = e.recibos().entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entrada -> entrada.getValue().id()));
        var relatorio = new Relatorio(e.plano().execucao(),cenario,completo,limpo,recibos,
                e.pendenciasSemRecibo().size(),diagnostico.sqlState(),completo ? resultado : null,diagnostico);
        if (!completo || !limpo) throw new Interrompida(relatorio);
        return relatorio;
    }
    private CustodiaPostgresHomologacao19() { }
}
