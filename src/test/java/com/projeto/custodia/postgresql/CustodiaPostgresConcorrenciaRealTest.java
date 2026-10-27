package com.projeto.custodia.postgresql;

import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.env.StandardEnvironment;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;

/** Opt-in exclusivo: nenhuma inicialização externa durante descoberta dos testes. */
@EnabledIfSystemProperty(named="homologacao.custodia.real",matches="EXECUTAR_3_10")
class CustodiaPostgresConcorrenciaRealTest {
    record Configuracao(PinPlataforma pin,Path docker,int porta){
        static Configuracao ler(Properties p){
            exigir("EXECUTAR_3_10".equals(p.getProperty("homologacao.custodia.real")),"Ativação 3.10 obrigatória");
            var pin=new PinPlataforma(new Imagem(IMAGEM,parametro(p,"digest")),parametro(p,"arquitetura"),"linux");
            int porta;
            try{porta=Integer.parseInt(parametro(p,"porta"));}
            catch(NumberFormatException e){throw new IllegalStateException("Porta inválida");}
            exigir(porta>=1024 && porta<=65535 && porta!=5432,"Porta exclusiva obrigatória");
            var docker=Path.of(parametro(p,"dockerExe"));
            exigir(docker.isAbsolute() && "docker.exe".equalsIgnoreCase(docker.getFileName().toString()),"Executável Docker local absoluto obrigatório");
            return new Configuracao(pin,docker,porta);
        }
        private static String parametro(Properties p,String nome){
            String v=p.getProperty("homologacao.custodia."+nome);
            exigir(v!=null && !v.isBlank(),"Parâmetro obrigatório ausente: "+nome);return v;
        }
    }
    @Test void matrizConcorrenteDescartavel() {
        var config=Configuracao.ler(System.getProperties());
        var env=new StandardEnvironment();env.setActiveProfiles(CustodiaPostgresSeguranca.PERFIL);
        var observador=new CustodiaPostgresContextoConcorrencia.Observador();
        var jdbc=CustodiaPostgresJdbc.driverLocal();
        var referencia=new java.util.concurrent.atomic.AtomicReference<Execucao>();
        var execucao=preparar(config.pin(),config.porta(),env,CustodiaPostgresDockerCli.localWindows(config.docker()),
                portaLoopback(),jdbc,(url,usuario,senha)->observador.acompanhar(jdbc.conector(referencia.get().plano()).abrir(url,usuario,senha)));
        referencia.set(execucao);
        boolean completo=false,limpo=false;String categoria="PREPARACAO";
        CustodiaPostgresConcorrencia matriz=null;
        org.springframework.context.annotation.AnnotationConfigApplicationContext contexto=null;
        try{
            execucao.provisionar();
            var m=execucao.manifesto();
            CustodiaPostgresSeguranca.identidade(m,config.pin().imagem(),execucao.inspecionar(m));
            categoria="MATRIZ";
            contexto=new org.springframework.context.annotation.AnnotationConfigApplicationContext();
                contexto.setEnvironment(env);
                contexto.registerBean(CustodiaPostgresContextoConcorrencia.Observador.class,()->observador);
                execucao.initializer().initialize(contexto);
                contexto.register(CustodiaPostgresContextoConcorrencia.class);contexto.refresh();
                matriz=new CustodiaPostgresConcorrencia(execucao,contexto,observador);matriz.executar();
            completo=true;categoria="CONCLUIDO";
        }catch(Exception|AssertionError falha){
            // Nunca encadear mensagem SQL, credenciais ou falha de transporte no relatório público.
            if(falha instanceof CustodiaPostgresConcorrencia.Interrompida segura){
                categoria=segura.caso;System.out.println("CUSTODIA_3_10 falha="+segura.categoria+" sqlState="+segura.sqlState);
            }
        }finally{
            try{
                exigir(matriz==null || matriz.atoresEncerrados,"Atores ainda ativos; limpeza bloqueada");
                if(contexto!=null)contexto.close();
                execucao.limpar();limpo=execucao.recibos().isEmpty() && execucao.pendenciasSemRecibo().isEmpty();
            }
            catch(Exception falha){categoria="LIMPEZA_INCONCLUSIVA";}
            System.out.println("CUSTODIA_3_10 uuid="+execucao.plano().execucao()+" completo="+completo+" limpo="+limpo
                    +" pendentes="+execucao.recibos().keySet()+" incertos="+execucao.pendenciasSemRecibo().size()+" caso="+categoria);
        }
        exigir(completo && limpo,"Executor 3.10 não aprovado; consultar relatório sanitizado");
    }
}
