package com.projeto.custodia.postgresql;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.env.StandardEnvironment;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresHomologacao19.*;

/** OPT-IN REAL. Cada cenário possui UUID, banco e recursos próprios; não executar nesta etapa. */
@EnabledIfSystemProperty(named="homologacao.custodia.real",matches="EXECUTAR_1_9")
class CustodiaPostgresHomologacao19RealTest {
    @Test void homologaInstalacaoEUpgradeEmExecucoesDescartaveisSeparadas(){
        var pin=new Imagem(IMAGEM,parametro("digest"));
        var plataforma=new PinPlataforma(pin,parametro("arquitetura"),"linux");
        var dockerExe=Path.of(parametro("dockerExe"));int porta=Integer.parseInt(parametro("porta"));
        for(var cenario:Cenario.values()){
            var env=new StandardEnvironment();env.setActiveProfiles(CustodiaPostgresSeguranca.PERFIL);
            var e=preparar(plataforma,porta,env,CustodiaPostgresDockerCli.localWindows(dockerExe),portaLoopback(),CustodiaPostgresJdbc.driverLocal());
            System.out.println("CUSTODIA_1_9 inicio="+cenario+" uuid="+e.plano().execucao()+" imagem="+pin.fixada()+" plataforma=linux/"+plataforma.arquitetura());
            System.out.println("CUSTODIA_1_9 "+executar(e,pin,cenario,CustodiaPostgresMigracoes19::executar));
        }
    }
    private static String parametro(String nome){var valor=System.getProperty("homologacao.custodia."+nome);
        exigir(valor!=null && !valor.isBlank(),"Parâmetro obrigatório ausente: "+nome);return valor;}
}
