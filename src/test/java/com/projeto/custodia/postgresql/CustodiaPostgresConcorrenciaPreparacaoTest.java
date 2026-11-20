package com.projeto.custodia.postgresql;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.Properties;
import java.sql.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustodiaPostgresConcorrenciaPreparacaoTest {
    @Test void caminhoDockerRelativoEhRecusadoAntesDeQualquerTransporte(){
        var p=configuracao();p.setProperty("homologacao.custodia.dockerExe","docker.exe");
        assertThrows(IllegalStateException.class,()->CustodiaPostgresConcorrenciaRealTest.Configuracao.ler(p));
    }
    static Properties configuracao(){
        var p=new Properties();p.setProperty("homologacao.custodia.real","EXECUTAR_3_10");
        p.setProperty("homologacao.custodia.digest","sha256:"+"a".repeat(64));
        p.setProperty("homologacao.custodia.arquitetura","amd64");
        p.setProperty("homologacao.custodia.porta","55437");
        p.setProperty("homologacao.custodia.dockerExe","C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe");return p;
    }
    @Test void fechamentoLiberaAtorQueAdquiriuLock() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (var barreira = new CustodiaPostgresCoordenacao(Duration.ofSeconds(2))) {
            var ator = executor.submit(barreira::aposLock);
            barreira.aguardarLock();
            assertFalse(ator.isDone());
            barreira.close();
            ator.get(2, TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS)); }
    }
    @ParameterizedTest @ValueSource(strings={"", "EXECUTAR_1_9","EXECUTAR_1_10","executar_3_10"})
    void ativacaoPrecisaSerExclusiva(String ativacao){
        var p=configuracao();p.setProperty("homologacao.custodia.real",ativacao);
        assertThrows(IllegalStateException.class,()->CustodiaPostgresConcorrenciaRealTest.Configuracao.ler(p));
    }
    @ParameterizedTest @ValueSource(strings={"digest","arquitetura","porta","dockerExe"})
    void parametroAusenteEhRecusado(String nome){
        var p=configuracao();p.remove("homologacao.custodia."+nome);
        assertThrows(IllegalStateException.class,()->CustodiaPostgresConcorrenciaRealTest.Configuracao.ler(p));
    }
    @ParameterizedTest @ValueSource(strings={"5432","0","65536","senha=segredo"})
    void portaInvalidaNaoExpoeValorRecebido(String porta){
        var p=configuracao();p.setProperty("homologacao.custodia.porta",porta);
        var erro=assertThrows(IllegalStateException.class,()->CustodiaPostgresConcorrenciaRealTest.Configuracao.ler(p));
        assertFalse(erro.toString().contains("segredo"));assertNull(erro.getCause());
    }
    @Test void digestEArquiteturaInvalidosSaoRecusados(){
        var p=configuracao();p.setProperty("homologacao.custodia.digest","latest");
        assertThrows(IllegalStateException.class,()->CustodiaPostgresConcorrenciaRealTest.Configuracao.ler(p));
        final var invalida=configuracao();invalida.setProperty("homologacao.custodia.arquitetura","x86");
        assertThrows(IllegalStateException.class,()->CustodiaPostgresConcorrenciaRealTest.Configuracao.ler(invalida));
    }
    @Test void configuracaoValidaNaoAbreConexoes(){
        assertEquals(55437,CustodiaPostgresConcorrenciaRealTest.Configuracao.ler(configuracao()).porta());
    }
    @Test void ausenciaDeSinalTemPrazoLimitado(){
        try(var b=new CustodiaPostgresCoordenacao(Duration.ofMillis(20))){
            assertEquals("BARREIRA_TIMEOUT",assertThrows(IllegalStateException.class,b::aguardarLock).getMessage());
        }
    }
    @Test void interrupcaoEncerraAtorEBloqueioSemPerderFlag()throws Exception{
        var flag=new java.util.concurrent.atomic.AtomicBoolean();
        var t=new Thread(()->{try(var b=new CustodiaPostgresCoordenacao(Duration.ofSeconds(2))){
            Thread.currentThread().interrupt();
            assertThrows(IllegalStateException.class,b::aguardarLock);flag.set(Thread.currentThread().isInterrupted());
        }});
        t.start();t.join(3000);assertFalse(t.isAlive());assertTrue(flag.get());
    }
    @Test void encerramentoInterrompeTrabalhoPendente()throws Exception{
        var iniciou=new CountDownLatch(1);var executor=Executors.newSingleThreadExecutor();
        executor.submit(()->{iniciou.countDown();try{new CountDownLatch(1).await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
        assertTrue(iniciou.await(1,TimeUnit.SECONDS));CustodiaPostgresConcorrencia.encerrar(executor);assertTrue(executor.isTerminated());
    }
    @Test void observadorDeCommitContaTentativaRealSemRepetirOuEncadearDiagnostico()throws Exception{
        var c=org.mockito.Mockito.mock(Connection.class);var pg=org.mockito.Mockito.mock(org.postgresql.PGConnection.class);
        org.mockito.Mockito.when(c.unwrap(org.postgresql.PGConnection.class)).thenReturn(pg);
        org.mockito.Mockito.when(pg.getBackendPID()).thenReturn(123);
        org.mockito.Mockito.doThrow(new SQLException("senha=segredo","40001")).when(c).commit();
        var o=new CustodiaPostgresContextoConcorrencia.Observador();
        var a=new CustodiaPostgresContextoConcorrencia.Ator("SIMULADO",null);
        o.executar(a,()->{var observado=o.acompanhar(c);assertThrows(SQLException.class,observado::commit);return null;});
        assertEquals(123,a.pid);assertEquals(1,a.commits.get());assertEquals("40001",a.estadoCommit);
        org.mockito.Mockito.verify(c,org.mockito.Mockito.times(1)).commit();assertNull(o.atual.get());
        var segura=new CustodiaPostgresConcorrencia.Interrompida("COMMIT_OPERACAO");
        assertNull(segura.getCause());assertFalse(segura.toString().contains("segredo"));
    }
    @Test void identificadorDesconhecidoNaoEntraNoRelatorio(){
        assertEquals("NA",CustodiaPostgresConcorrencia.codigoSeguro("senha=segredo jdbc:postgresql://externo"));
        assertEquals("ESTORNO_DUPLICADO",CustodiaPostgresConcorrencia.codigoSeguro("ESTORNO_DUPLICADO"));
    }
}
