package com.projeto.custodia.postgresql;

import com.projeto.entities.*;
import com.projeto.services.*;
import com.projeto.dto.*;
import com.projeto.services.exceptions.*;
import com.projeto.repositories.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresContextoConcorrencia.*;

/** Matriz de serviços reais. Não contém comandos de provisionamento ou destino configurável. */
final class CustodiaPostgresConcorrencia {
    static final Duration PRAZO=Duration.ofSeconds(20);
    static final LocalDate DIA=LocalDate.of(2026,10,1);
    final CustodiaPostgresOperacional.Execucao execucao;
    final ApplicationContext contexto;
    final Observador observador;
    final EntityManagerFactory emf;
    final PlatformTransactionManager manager;
    String caso="CONTEXTO";
    volatile boolean atoresEncerrados=true;
    static final class Interrompida extends IllegalStateException {
        final String caso;
        final String categoria,sqlState;
        Interrompida(String caso){this(caso,"EXECUCAO","NA");}
        Interrompida(String caso,String categoria,String sqlState){super("Cenário 3.10 interrompido: "+caso+" categoria="+categoria+" sqlState="+sqlState);this.caso=caso;this.categoria=categoria;this.sqlState=sqlState;}
    }
    record Fixture(long carteira,long outra,long acao,long a,long b,long c,Long original){}
    record Resultado(String codigo,String motivo){}
    CustodiaPostgresConcorrencia(CustodiaPostgresOperacional.Execucao execucao,ApplicationContext contexto,Observador observador){
        this.execucao=execucao;this.contexto=contexto;this.observador=observador;
        emf=contexto.getBean(EntityManagerFactory.class);manager=contexto.getBean(PlatformTransactionManager.class);
    }
    void executar(){
        try{
            for(String tipo:List.of("TRANSFERENCIAS","TRANSFERENCIA_VENDA","ATRIBUICAO_VENDA","ESTORNO_VENDA",
                    "ESTORNO_TRANSFERENCIA","ESTORNOS","COMPRA_EXCLUSAO","CUSTODIA_EXCLUSAO","INDEPENDENTES"))
                for(boolean inversa:List.of(false,true))cenario(tipo,inversa);
            for(String tipo:List.of("TRANSFERENCIA","ATRIBUICAO","ESTORNO","VENDA","EXCLUSAO"))timeout(tipo);
            for(String tipo:List.of("CUSTODIA","OPERACAO","EXCLUSAO"))commitRejeitado(tipo);
        }catch(Exception|AssertionError e){
            var d=CustodiaPostgresHomologacao19.diagnosticar(CustodiaPostgresHomologacao19.CasoFalha.ROTEIRO,e);
            throw new Interrompida(caso,d.categoria().name(),d.sqlState());
        }
    }
    <T>T tx(Function<EntityManager,T> trabalho){
        var t=new TransactionTemplate(manager);t.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return t.execute(s->trabalho.apply(Objects.requireNonNull(EntityManagerFactoryUtils.getTransactionalEntityManager(emf))));
    }
    Fixture fixture(boolean nula,boolean vazia,boolean original){
        return tx(em->{
            for(String entidade:List.of("EstornoMovimentacaoCustodia","MovimentacaoCustodia","Operacao","SnapshotCarteira","Acao","Corretora","Carteira"))
                em.createQuery("delete from "+entidade).executeUpdate();
            var data=OffsetDateTime.ofInstant(AGORA,ZoneOffset.UTC);
            var carteira=new Carteira("Alvo 3.10",data);var outra=new Carteira("Independente 3.10",data);
            var acao=new Acao("PETR4","Sintética",Mercado.BRASIL,Moeda.BRL,BigDecimal.TEN,data);
            var a=corretora("11111111000111",data);var b=corretora("22222222000122",data);var c=corretora("33333333000133",data);
            for(Object v:List.of(carteira,outra,acao,a,b,c))em.persist(v);
            if(!vazia)em.persist(new Operacao(carteira,acao,nula?null:a,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,1,new BigDecimal("1000")));
            em.persist(new Operacao(outra,acao,a,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,1,new BigDecimal("1000")));
            Long id=null;
            if(original){
                var m=new MovimentacaoCustodia(null,carteira,acao,NaturezaMovimentacaoCustodia.TRANSFERENCIA,a,b,new BigDecimal("40"),
                        DIA,1,PosicaoRacionalCustodia.deTexto("1","2"),Clock.fixed(AGORA.minusSeconds(60),ZoneOffset.UTC));
                em.persist(m);id=m.getId();
            }
            em.flush();return new Fixture(carteira.getId(),outra.getId(),acao.getId(),a.getId(),b.getId(),c.getId(),id);
        });
    }
    static Corretora corretora(String cnpj,OffsetDateTime data){
        return new Corretora(cnpj,"Sintética","Teste",null,null,"01001000","Rua","1",null,"Centro","São Paulo","SP","ATIVA",data);
    }
    void transferir(Fixture f,long carteira,long origem,long destino,String quantidade){
        contexto.getBean(TransferenciaCustodiaService.class).transferir(new TransferenciaCustodiaCommand(carteira,"PETR4",Mercado.BRASIL,origem,destino,new BigDecimal(quantidade),DIA));
    }
    void atribuir(Fixture f){contexto.getBean(AtribuicaoCustodiaService.class).atribuir(new AtribuicaoCustodiaCommand(f.carteira(),"PETR4",Mercado.BRASIL,f.b(),new BigDecimal("70"),DIA));}
    void estornar(Fixture f){contexto.getBean(EstornoCustodiaService.class).estornar(new EstornoCustodiaCommand(f.carteira(),f.original(),"Concorrência 3.10"));}
    void operacao(Fixture f,boolean compra,Long corretora,String quantidade,LocalDate dia){
        OperacaoCreateRequest request=compra
                ?new OperacaoCompraCreateRequest(f.carteira(),"PETR4",Mercado.BRASIL,corretora,new BigDecimal(quantidade),dia,BigDecimal.TEN)
                :new OperacaoVendaCreateRequest(f.carteira(),"PETR4",Mercado.BRASIL,corretora,new BigDecimal(quantidade),dia,BigDecimal.TEN);
        contexto.getBean(OperacaoService.class).cadastrar(request);
    }
    void excluir(Fixture f){contexto.getBean(CarteiraService.class).excluir(f.carteira());}
    Resultado chamar(Ator ator,Runnable chamada)throws Exception{
        return observador.executar(ator,()->{
            try{chamada.run();return new Resultado("OK",null);}
            catch(ApiException e){assertEquals(409,e.getStatus().value());return new Resultado(e.getCode(),(String)e.getDetails().get("motivo"));}
            catch(ObjectNotFoundException e){return new Resultado("404",null);}
        });
    }
    void cenario(String tipo,boolean inversa)throws Exception{
        caso=tipo+(inversa?"_INVERSA":"_DIRETA");
        System.out.println("CUSTODIA_3_10 inicio="+caso);
        var f=fixture(tipo.equals("ATRIBUICAO_VENDA"),tipo.equals("COMPRA_EXCLUSAO"),tipo.startsWith("ESTORNO"));
        var financeiro=financeiro(f);
        Runnable esquerda,direita;
        String erroEsquerda=null,erroDireita=null,motivo=null;
        switch(tipo){
            case "TRANSFERENCIAS"->{
                esquerda=()->transferir(f,f.carteira(),f.a(),f.b(),"70");direita=()->transferir(f,f.carteira(),f.a(),f.c(),"70");
                erroEsquerda=erroDireita="POSICAO_CUSTODIA_INDISPONIVEL";motivo="MOVIMENTACAO_CUSTODIA_INVALIDADA";
            }
            case "TRANSFERENCIA_VENDA","ATRIBUICAO_VENDA"->{
                esquerda=tipo.equals("ATRIBUICAO_VENDA")?()->atribuir(f):()->transferir(f,f.carteira(),f.a(),f.b(),"70");
                direita=()->operacao(f,false,tipo.equals("ATRIBUICAO_VENDA")?null:f.a(),"70",DIA);
                erroEsquerda="POSICAO_CUSTODIA_INDISPONIVEL";erroDireita="SALDO_CORRETORA_INSUFICIENTE";
                if(inversa)motivo="CONFLITO_HISTORICO_QUANTITATIVO";
            }
            case "ESTORNO_VENDA"->{
                esquerda=()->estornar(f);direita=()->operacao(f,false,f.b(),"30",DIA.plusDays(1));
                erroEsquerda="CONFLITO_HISTORICO_QUANTITATIVO";erroDireita="SALDO_CORRETORA_INSUFICIENTE";
            }
            case "ESTORNO_TRANSFERENCIA"->{
                esquerda=()->estornar(f);direita=()->transferir(f,f.carteira(),f.b(),f.c(),"30");
                erroEsquerda="MOVIMENTACAO_CUSTODIA_INVALIDADA";erroDireita="SALDO_ORIGEM_INSUFICIENTE";
            }
            case "ESTORNOS"->{esquerda=()->estornar(f);direita=()->estornar(f);erroEsquerda=erroDireita="ESTORNO_DUPLICADO";}
            case "COMPRA_EXCLUSAO"->{esquerda=()->operacao(f,true,f.a(),"10",DIA);direita=()->excluir(f);erroEsquerda="404";erroDireita="CARTEIRA_POSSUI_OPERACOES";}
            case "CUSTODIA_EXCLUSAO"->{esquerda=()->transferir(f,f.carteira(),f.a(),f.b(),"40");direita=()->excluir(f);erroDireita="CARTEIRA_POSSUI_OPERACOES";}
            case "INDEPENDENTES"->{esquerda=()->transferir(f,f.carteira(),f.a(),f.b(),"40");direita=()->transferir(f,f.outra(),f.a(),f.b(),"40");}
            default->throw new AssertionError("CENARIO_DESCONHECIDO");
        }
        Runnable primeiro=inversa?direita:esquerda,segundo=inversa?esquerda:direita;
        String esperadoPrimeiro=tipo.equals("CUSTODIA_EXCLUSAO") && inversa?erroDireita:"OK";
        String esperadoSegundo=inversa?erroEsquerda:erroDireita;
        if(esperadoSegundo==null)esperadoSegundo="OK";
        try(var observacao=execucao.datasource().getConnection();var barreira=new CustodiaPostgresCoordenacao(PRAZO)){
            var a=new Ator("A",barreira);var b=new Ator("B",null);
            var threads=novosAtores();
            try{
                var fa=threads.submit(()->chamar(a,primeiro));barreira.aguardarLock();
                var fb=threads.submit(()->chamar(b,segundo));
                if(tipo.equals("INDEPENDENTES")){assertEquals("OK",fb.get(20,TimeUnit.SECONDS).codigo());assertFalse(fa.isDone());}
                else observarEspera(observacao,a,b,fb);
                barreira.close();
                var resultadoA=fa.get(20,TimeUnit.SECONDS);var resultado=fb.get(20,TimeUnit.SECONDS);
                System.out.println("CUSTODIA_3_10 caso="+caso+" codigoA="+codigoSeguro(resultadoA.codigo())+" codigoB="+codigoSeguro(resultado.codigo())+" causaB="+codigoSeguro(resultado.motivo()));
                assertEquals(esperadoPrimeiro,resultadoA.codigo());assertEquals(esperadoSegundo,resultado.codigo());
                if(motivo!=null)assertEquals(motivo,resultado.motivo());
                assertEquals(1,a.locks.get());
                assertEquals(1,b.consultas.stream().filter("findByIdForUpdate"::equals).count());
                if(!resultado.codigo().equals("404"))assertEquals(1,b.locks.get());
                if(!tipo.equals("INDEPENDENTES") && !resultado.codigo().equals("404"))assertTrue(b.consultas.size()>1);
                verificarFinal(tipo,inversa,f,financeiro);
                prova(a,b,"OK");
            }finally{barreira.close();encerrarAtores(threads);}
        }
    }
    static void encerrar(ExecutorService threads)throws InterruptedException{
        threads.shutdownNow();
        if(!threads.awaitTermination(25,TimeUnit.SECONDS))throw new IllegalStateException("ATORES_NAO_ENCERRADOS");
    }
    ExecutorService novosAtores(){
        atoresEncerrados=false;
        return Executors.newFixedThreadPool(2,r->{var t=new Thread(r,"custodia-310-ator");t.setDaemon(true);return t;});
    }
    void encerrarAtores(ExecutorService threads)throws InterruptedException{encerrar(threads);atoresEncerrados=true;}
    static String codigoSeguro(String codigo){
        return codigo!=null && Set.of("OK","404","POSICAO_CUSTODIA_INDISPONIVEL","SALDO_ORIGEM_INSUFICIENTE",
                "SALDO_CORRETORA_INSUFICIENTE","MOVIMENTACAO_CUSTODIA_INVALIDADA","CONFLITO_HISTORICO_QUANTITATIVO",
                "ESTORNO_DUPLICADO","CARTEIRA_POSSUI_OPERACOES","CONFLITO_TRANSACIONAL_TRANSITORIO").contains(codigo)?codigo:"NA";
    }
    void observarEspera(Connection c,Ator a,Ator b,Future<?> futuro)throws Exception{
        assertTrue(b.conectado.await(10,TimeUnit.SECONDS));assertNotEquals(a.pid,b.pid);
        long limite=System.nanoTime()+Duration.ofSeconds(8).toNanos();
        try(var s=c.prepareStatement(
                "select ? = any(pg_blocking_pids(pid)), wait_event_type from pg_stat_activity where pid=?")){
            do{
                s.setInt(1,a.pid);s.setInt(2,b.pid);
                try(var r=s.executeQuery()){if(r.next() && r.getBoolean(1) && "Lock".equals(r.getString(2))){
                    System.out.println("CUSTODIA_3_10 caso="+caso+" bloqueador="+a.pid+" aguardando="+b.pid+" esperaComprovada=true");return;
                }}
                if(futuro.isDone())throw new AssertionError("ATOR_TERMINOU_SEM_ESPERA");
                java.util.concurrent.locks.LockSupport.parkNanos(Duration.ofMillis(20).toNanos());
            }while(System.nanoTime()<limite);
        }
        throw new AssertionError("ESPERA_NAO_COMPROVADA");
    }
    List<?> financeiro(Fixture f){return tx(em->em.createNativeQuery("select id,carteira_id,acao_id,corretora_id,tipo,quantidade,preco_unitario,data_operacao,ordem_no_dia,valor_total from operacao where carteira_id="+f.carteira()+" order by id").getResultList().stream().map(v->Arrays.asList((Object[])v)).toList());}
    long contar(String entidade,long carteira){return tx(em->em.createQuery("select count(x) from "+entidade+" x where "+(entidade.equals("EstornoMovimentacaoCustodia")?"x.movimentacaoOriginal.carteira.id":"x.carteira.id")+"=:id",Long.class).setParameter("id",carteira).getSingleResult());}
    void verificarFinal(String tipo,boolean inversa,Fixture f,List<?> antes){
        long movimentos= switch(tipo){case "TRANSFERENCIAS","CUSTODIA_EXCLUSAO","INDEPENDENTES"->1;case "TRANSFERENCIA_VENDA","ATRIBUICAO_VENDA"->inversa?0:1;case "ESTORNO_TRANSFERENCIA"->inversa?2:1;case "ESTORNO_VENDA","ESTORNOS"->1;default->0;};
        assertEquals(movimentos,contar("MovimentacaoCustodia",f.carteira()));
        assertEquals(tipo.equals("ESTORNOS") || ((tipo.equals("ESTORNO_VENDA") || tipo.equals("ESTORNO_TRANSFERENCIA")) && !inversa)?1:0,contar("EstornoMovimentacaoCustodia",f.carteira()));
        boolean vendaConfirmada=inversa && Set.of("TRANSFERENCIA_VENDA","ATRIBUICAO_VENDA","ESTORNO_VENDA").contains(tipo);
        if(tipo.equals("COMPRA_EXCLUSAO")){
            assertEquals(inversa?0:1,contar("Operacao",f.carteira()));
            assertEquals(!inversa,tx(em->em.find(Carteira.class,f.carteira())!=null));
        }else if(vendaConfirmada){assertEquals(2,contar("Operacao",f.carteira()));assertTrue(financeiro(f).containsAll(antes));}
        else assertEquals(antes,financeiro(f));
        assertEquals(tipo.equals("INDEPENDENTES")?1:0,contar("MovimentacaoCustodia",f.outra()));
        assertEquals(1,contar("Operacao",f.outra()));
        String[] esperado=switch(tipo){
            case "TRANSFERENCIAS"->inversa?new String[]{"30","0","70","0"}:new String[]{"30","70","0","0"};
            case "TRANSFERENCIA_VENDA"->inversa?new String[]{"30","0","0","0"}:new String[]{"30","70","0","0"};
            case "ATRIBUICAO_VENDA"->inversa?new String[]{"0","0","0","30"}:new String[]{"0","70","0","30"};
            case "ESTORNO_VENDA"->inversa?new String[]{"60","10","0","0"}:new String[]{"100","0","0","0"};
            case "ESTORNO_TRANSFERENCIA"->inversa?new String[]{"60","10","30","0"}:new String[]{"100","0","0","0"};
            case "ESTORNOS"->new String[]{"100","0","0","0"};
            case "COMPRA_EXCLUSAO"->new String[]{inversa?"0":"10","0","0","0"};
            default->new String[]{"60","40","0","0"};
        };
        conferirSaldos(f,f.carteira(),esperado);
        conferirSaldos(f,f.outra(),tipo.equals("INDEPENDENTES")?new String[]{"60","40","0","0"}:new String[]{"100","0","0","0"});
    }
    void conferirSaldos(Fixture f,long carteira,String[] esperado){tx(em->{
        var financeiros=contexto.getBean(OperacaoRepository.class).listarParaReplayQuantitativo(carteira,f.acao());
        var originais=contexto.getBean(MovimentacaoCustodiaRepository.class).listarPorCarteiraEAcao(carteira,f.acao());
        var ids=originais.stream().map(MovimentacaoCustodia::getId).toList();
        var estornos=ids.isEmpty()?List.<EstornoMovimentacaoCustodia>of():contexto.getBean(EstornoMovimentacaoCustodiaRepository.class).listarPorCarteiraEOriginais(carteira,ids);
        var vinculos=originais.stream().map(m->estornos.stream().filter(e->e.getMovimentacaoOriginal().getId().equals(m.getId())).findFirst()
                .map(e->VinculoEstornoConsultado.vinculoPresente(m.getId(),e)).orElseGet(()->VinculoEstornoConsultado.ausenciaVerificada(m.getId()))).toList();
        var fontes=new AdaptadorEventosQuantitativos().adaptar(carteira,f.acao(),financeiros,originais,vinculos);
        var eventos=new NormalizadorEventosQuantitativos().normalizar(carteira,f.acao(),fontes.financeiros(),fontes.custodias());
        var replay=new MotorReplayQuantitativo().reproduzir(eventos);
        Long[] grupos={f.a(),f.b(),f.c(),null};BigDecimal soma=BigDecimal.ZERO;
        for(int i=0;i<grupos.length;i++){var valor=new BigDecimal(esperado[i]);soma=soma.add(valor);assertEquals(0,valor.compareTo(replay.estadoFinal().saldo(new EventoQuantitativo.Grupo(grupos[i]))));}
        assertEquals(0,soma.compareTo(replay.estadoFinal().quantidadeConsolidada()));assertTrue(replay.estadoFinal().reconciliada());
        assertTrue(replay.checkpoints().stream().allMatch(p->p.estado().primeirosNegativos().isEmpty()));return null;
    });}
    void prova(Ator a,Ator b,String resultado){System.out.println("CUSTODIA_3_10 caso="+caso+" pidA="+a.pid+" pidB="+b.pid+" resultado="+resultado+" commitsA="+a.commits+" commitsB="+b.commits);}

    void timeout(String tipo)throws Exception{
        caso="TIMEOUT_"+tipo;var f=fixture(tipo.equals("ATRIBUICAO"),false,tipo.equals("ESTORNO"));
        var antes=financeiro(f);long movimentos=contar("MovimentacaoCustodia",f.carteira());
        try(var observacao=execucao.datasource().getConnection();var barreira=new CustodiaPostgresCoordenacao(PRAZO)){
            var a=new Ator("A",barreira);var b=new Ator("B",null);var threads=novosAtores();
            try{
                var fa=threads.submit(()->observador.executar(a,()->tx(em->{em.find(Carteira.class,f.carteira(),LockModeType.PESSIMISTIC_WRITE);barreira.aposLock();return null;})));
                barreira.aguardarLock();
                var fb=threads.submit(()->observador.executar(b,()->{
                    var t=new TransactionTemplate(manager);t.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                    try{t.execute(s->{var em=EntityManagerFactoryUtils.getTransactionalEntityManager(emf);em.createNativeQuery("SET LOCAL lock_timeout='3s'").executeUpdate();
                        switch(tipo){case "TRANSFERENCIA"->transferir(f,f.carteira(),f.a(),f.b(),"10");case "ATRIBUICAO"->atribuir(f);case "ESTORNO"->estornar(f);case "VENDA"->operacao(f,false,f.a(),"1",DIA);case "EXCLUSAO"->excluir(f);default->throw new AssertionError();}return null;});throw new AssertionError("TIMEOUT_AUSENTE");}
                    catch(ApiException e){assertEquals("CONFLITO_TRANSACIONAL_TRANSITORIO",e.getCode());assertNull(e.getCause());return null;}
                }));
                observarEspera(observacao,a,b,fb);fb.get(10,TimeUnit.SECONDS);barreira.close();fa.get(20,TimeUnit.SECONDS);
                assertEquals("55P03",b.ultimoSqlState);
                assertEquals(List.of("findByIdForUpdate"),b.consultas);assertEquals(antes,financeiro(f));
                assertEquals(movimentos,contar("MovimentacaoCustodia",f.carteira()));assertEquals(0,contar("EstornoMovimentacaoCustodia",f.carteira()));
                prova(a,b,"55P03_TRADUZIDO");
            }finally{barreira.close();encerrarAtores(threads);}
        }
    }
    void commitRejeitado(String tipo)throws Exception{
        caso="COMMIT_"+tipo;var f=fixture(false,tipo.equals("EXCLUSAO"),false);var antes=financeiro(f);
        var m=execucao.manifesto();CustodiaPostgresSeguranca.identidade(m,m.plano().imagem(),execucao.inspecionar(m));
        String tabela=tipo.equals("CUSTODIA")?"movimentacao_custodia":tipo.equals("OPERACAO")?"operacao":"carteira";
        String evento=tipo.equals("EXCLUSAO")?"DELETE":"INSERT";
        String coluna=tipo.equals("EXCLUSAO")?"OLD.id":"NEW.carteira_id";
        String funcao="custodia_310_rejeitar_commit";
        try(var c=execucao.datasource().getConnection();var s=c.createStatement()){
            s.execute("CREATE FUNCTION "+funcao+"() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION USING ERRCODE='40001', MESSAGE='CUSTODIA_310_COMMIT_INJETADO'; END; $$");
            s.execute("CREATE CONSTRAINT TRIGGER custodia_310_commit AFTER "+evento+" ON "+tabela+" DEFERRABLE INITIALLY DEFERRED FOR EACH ROW WHEN ("+coluna+"="+f.carteira()+") EXECUTE FUNCTION "+funcao+"()");
        }
        var ator=new Ator("COMMIT",null);
        try{
            var resultado=chamar(ator,()->{
                if(tipo.equals("CUSTODIA"))transferir(f,f.carteira(),f.a(),f.b(),"40");
                else if(tipo.equals("OPERACAO"))operacao(f,true,f.a(),"10",DIA);
                else excluir(f);
            });
            assertEquals("CONFLITO_TRANSACIONAL_TRANSITORIO",resultado.codigo());
            assertEquals(1,ator.commits.get());assertEquals("40001",ator.estadoCommit);
            assertTrue(ator.dml.get()>0,"DML precisa ter sido aceito antes da tentativa de COMMIT");
            assertEquals(1,ator.locks.get());
            if(!tipo.equals("EXCLUSAO"))assertEquals(1,ator.flushes.get());
            assertEquals(tipo.equals("EXCLUSAO")?"delete":"saveAndFlush",ator.consultas.get(ator.consultas.size()-1));
            assertEquals(antes,financeiro(f));assertEquals(0,contar("MovimentacaoCustodia",f.carteira()));
            assertEquals(0,contar("EstornoMovimentacaoCustodia",f.carteira()));
            assertEquals(Boolean.TRUE,tx(em->em.find(Carteira.class,f.carteira())!=null));
            System.out.println("CUSTODIA_3_10 caso="+caso+" commitTentado=true sqlState=40001 injecaoServidor=true rollback=true");
        }finally{
            try(var c=execucao.datasource().getConnection();var s=c.createStatement()){
                s.execute("DROP TRIGGER custodia_310_commit ON "+tabela);s.execute("DROP FUNCTION "+funcao+"()");
            }
        }
    }
}
