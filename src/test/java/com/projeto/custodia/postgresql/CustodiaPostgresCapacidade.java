package com.projeto.custodia.postgresql;

import java.sql.Connection;
import java.util.Set;
import java.util.Map;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;

/** Executor exclusivo de 1.10; nenhuma operação ocorre ao carregar a classe. */
public final class CustodiaPostgresCapacidade {
    enum Etapa { PROVISIONAMENTO, IDENTIDADE, EXPERIMENTO, CONCLUIDO }
    record Relatorio(java.util.UUID execucao, String imagem, Etapa etapa, boolean experimentoCompleto,
            boolean limpezaCompleta, Map<Tipo,String> recursosPendentes, int criacoesIncertas,
            String sqlState, Resultado resultado) {
        Relatorio { recursosPendentes = Map.copyOf(recursosPendentes); }
    }
    static final class Interrompida extends IllegalStateException {
        private final Relatorio relatorio;
        Interrompida(Relatorio r) { super("Homologação 1.10 não aprovada: " + r); relatorio = r; }
        Relatorio relatorio() { return relatorio; }
    }
    enum Prova { ROUNDTRIP_512, COMPARACAO, DUPLICIDADE, EQUIVALENCIA, CHAVE_COMPOSTA, FORMATO, CRESCIMENTO, INDICE }
    record Resultado(int versao, int bloco, long bytesIndice, int roundtrips, int adversariais, Set<Prova> provas) {
        Resultado { provas = Set.copyOf(provas); }
        void validar() {
            exigir(versao == 170011 && bloco > 0 && bytesIndice > 0 && roundtrips == 2467
                    && adversariais == 2449 && provas.equals(Set.of(Prova.values())), "Evidências incompletas");
        }
    }
    @FunctionalInterface interface Experimento { Resultado executar(Connection c) throws Exception; }
    static Relatorio executar(Execucao execucao, Imagem pin, Experimento experimento) {
        exigir(execucao != null && pin != null && experimento != null, "Dependências explícitas obrigatórias");
        Resultado resultado = null;
        Etapa etapa = Etapa.PROVISIONAMENTO;
        boolean completa = false, limpa = false;
        String sqlState = "NA";
        try {
            exigir(pin.equals(execucao.plano().imagem()), "Pin independente divergente");
            execucao.provisionar();
            etapa = Etapa.IDENTIDADE;
            var m = execucao.manifesto();
            exigir(m != null && execucao.plano().equals(m.plano()), "Manifesto de outra execução");
            CustodiaPostgresSeguranca.identidade(m, pin, execucao.inspecionar(m));
            etapa = Etapa.EXPERIMENTO;
            try (var c = execucao.datasource().getConnection()) {
                resultado = experimento.executar(c);
                exigir(resultado != null, "Resultado ausente"); resultado.validar();
            }
            completa = true; etapa = Etapa.CONCLUIDO;
        } catch (Exception falha) {
            // Exceções de JDBC/Docker podem conter URL, SQL e credenciais.
            // Reportar somente campos tipados; nunca preservar causa/mensagem/suppressed.
            if (falha instanceof java.sql.SQLException sql && sql.getSQLState() != null
                    && sql.getSQLState().matches("[A-Z0-9]{5}")) sqlState = sql.getSQLState();
        } finally {
            try { execucao.limpar(); limpa = execucao.recibos().isEmpty() && execucao.pendenciasSemRecibo().isEmpty(); }
            catch (Exception falha) { /* Recibos permanecem no orquestrador original. */ }
        }
        var r = new Relatorio(execucao.plano().execucao(), pin.fixada(), etapa, completa, limpa,
                execucao.recibos().entrySet().stream().collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey, entrada -> entrada.getValue().id())),
                execucao.pendenciasSemRecibo().size(), sqlState, completa ? resultado : null);
        if (!completa || !limpa) throw new Interrompida(r);
        return r;
    }
    private CustodiaPostgresCapacidade() { }
}
