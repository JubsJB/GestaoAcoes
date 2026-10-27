package com.projeto.custodia.postgresql;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.env.StandardEnvironment;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;

/** OPT-IN REAL: não executar nesta etapa. Sem Spring Boot, migrations, Hibernate ou providers. */
@EnabledIfSystemProperty(named="homologacao.custodia.real", matches="EXECUTAR_1_10")
class CustodiaPostgresCapacidadeRealTest {
    @Test void homologaCapacidadeFisicaNoPostgres17Descartavel() throws Exception {
        var pin = new Imagem(IMAGEM, obrigatoria("digest"));
        var plataforma = new PinPlataforma(pin, obrigatoria("arquitetura"), "linux");
        var env = new StandardEnvironment(); env.setActiveProfiles(CustodiaPostgresSeguranca.PERFIL);
        var execucao = preparar(plataforma, Integer.parseInt(obrigatoria("porta")), env,
                CustodiaPostgresDockerCli.localWindows(Path.of(obrigatoria("dockerExe"))),
                portaLoopback(), CustodiaPostgresJdbc.driverLocal());
        var relatorio = CustodiaPostgresCapacidade.executar(execucao, pin, CustodiaPostgresExperimentoCapacidade::executar);
        System.out.println("CUSTODIA_1_10 uuid=" + execucao.plano().execucao() + " imagem=" + pin.fixada()
                + " plataforma=linux/" + plataforma.arquitetura() + " relatorio=" + relatorio
                + " recursosRemovidos=" + execucao.recibos().isEmpty());
        System.out.println("METODOLOGIA seed=11017 amostras512=8 crescimento=mediantaAlternada ddl="
                + CustodiaPostgresExperimentoCapacidade.DDL + " indice=(" + CustodiaPostgresExperimentoCapacidade.COLUNAS + ")");
    }
    private static String obrigatoria(String nome) {
        var valor = System.getProperty("homologacao.custodia." + nome);
        exigir(valor != null && !valor.isBlank(), "Parâmetro obrigatório ausente: " + nome); return valor;
    }
}
