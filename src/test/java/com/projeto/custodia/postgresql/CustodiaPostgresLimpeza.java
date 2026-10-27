package com.projeto.custodia.postgresql;

import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;

/** Só operações tipadas por ID exato. Não existe execução de shell, prune ou seleção por prefixo. */
public final class CustodiaPostgresLimpeza {
    public interface Remocao {
        void container(String id);
        void volume(String id);
        void rede(String id);
    }

    /** Recursos Hibernate/Spring consumidores devem ser encerrados antes de chamar esta rotina.
     * Datasource close impede novas conexões e fecha todas as conexões já emitidas.
     * Sem finally destrutivo: qualquer divergência ou erro interrompe a limpeza. */
    public static void executar(CustodiaPostgresManifesto m, Imagem pin, Inspecao inspecao,
            CustodiaPostgresDataSource ds, Remocao remocao) throws java.sql.SQLException {
        exigir(m != null && pin != null && inspecao != null && ds != null && remocao != null, "Limpeza incompleta");
        exigir(m.equals(ds.manifesto()), "Datasource de outra execução");
        CustodiaPostgresSeguranca.identidade(m, pin, inspecao.inspecionar(m));
        ds.close(); exigir(ds.encerradoSemConexoes(), "Conexões ainda abertas");
        CustodiaPostgresSeguranca.identidade(m, pin, inspecao.inspecionar(m));
        // Volume e rede usam seus identificadores exatos do manifesto; Docker identifica volume por nome.
        conferir(m, inspecao, Tipo.CONTAINER, m.containerId()); remocao.container(m.containerId());
        conferir(m, inspecao, Tipo.VOLUME, m.volumeId()); remocao.volume(m.volumeId());
        conferir(m, inspecao, Tipo.REDE, m.redeId()); remocao.rede(m.redeId());
    }
    private static void conferir(CustodiaPostgresManifesto m, Inspecao i, Tipo tipo, String id) {
        CustodiaPostgresSeguranca.recurso(m, tipo, i.inspecionarRecurso(tipo, id));
    }
    private CustodiaPostgresLimpeza() { }
}
