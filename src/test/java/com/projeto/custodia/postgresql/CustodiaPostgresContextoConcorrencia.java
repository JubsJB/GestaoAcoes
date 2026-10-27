package com.projeto.custodia.postgresql;

import com.projeto.services.*;
import com.projeto.mappers.*;
import com.projeto.validation.TickerNormalizer;
import com.projeto.services.exceptions.ConstraintNameExtractor;
import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Sem Boot/scan de serviços/providers. Apenas os escritores reais aprovados. */
@TestConfiguration(proxyBeanMethods=false)
@EnableTransactionManagement
@EnableJpaRepositories(basePackages="com.projeto.repositories")
@Import({CustodiaPostgresConfiguracao.Completa.class, TransferenciaCustodiaService.class,
        AtribuicaoCustodiaService.class, EstornoCustodiaService.class,
        TransferenciaCustodiaPersistenceService.class, OperacaoService.class,
        OperacaoPersistenceService.class, HistoricoQuantitativoCustodia.class,
        CalculadoraPosicao.class, TickerNormalizer.class, OperacaoMapper.class,
        CarteiraMapper.class, CarteiraService.class, CarteiraExclusaoPersistenceService.class,
        ConstraintNameExtractor.class})
class CustodiaPostgresContextoConcorrencia {
    static final Instant AGORA=Instant.parse("2026-10-24T12:00:00Z");
    @Bean Clock clock(){return Clock.fixed(AGORA,ZoneOffset.UTC);}
    @Bean JpaTransactionManager transactionManager(EntityManagerFactory emf){return new JpaTransactionManager(emf);}
    @Bean static BeanPostProcessor instrumentacao(Observador observador){
        return new BeanPostProcessor(){
            @Override public Object postProcessAfterInitialization(Object bean,String nome){
                if(!(bean instanceof org.springframework.data.repository.Repository))return bean;
                var proxy=new ProxyFactory(bean);
                proxy.addAdvice((MethodInterceptor) chamada->{
                    var ator=observador.atual.get();
                    if(ator==null)return chamada.proceed();
                    String metodo=chamada.getMethod().getName();
                    ator.consultas.add(metodo);
                    if(ator.consultas.size()==1 && !metodo.equals("findByIdForUpdate"))
                        throw new AssertionError("LOCK_NAO_FOI_PRIMEIRO_ACESSO");
                    Object resultado=chamada.proceed();
                    if(metodo.equals("saveAndFlush"))ator.flushes.incrementAndGet();
                    if(metodo.equals("findByIdForUpdate")){
                        ator.locks.incrementAndGet();
                        if(ator.conexao.getTransactionIsolation()!=Connection.TRANSACTION_READ_COMMITTED
                                || ator.conexao.isReadOnly() || ator.conexao.getAutoCommit())
                            throw new AssertionError("SESSAO_TRANSACIONAL_INCOMPATIVEL");
                        if(ator.barreira!=null)ator.barreira.aposLock();
                    }
                    return resultado;
                });
                return proxy.getProxy();
            }
        };
    }

    static final class Ator {
        final String nome;
        final CustodiaPostgresCoordenacao barreira;
        final List<String> consultas=new CopyOnWriteArrayList<>();
        final AtomicInteger locks=new AtomicInteger(), commits=new AtomicInteger(), flushes=new AtomicInteger();
        final AtomicInteger dml=new AtomicInteger();
        volatile int pid;
        volatile Connection conexao;
        volatile String estadoCommit="NA";
        volatile String ultimoSqlState="NA";
        final CountDownLatch conectado=new CountDownLatch(1);
        Ator(String nome,CustodiaPostgresCoordenacao barreira){this.nome=nome;this.barreira=barreira;}
    }
    /** Observa o JDBC real: nunca substitui resultado de SQL/commit. Não imprime SQL nem exceções. */
    static final class Observador {
        final ThreadLocal<Ator> atual=new ThreadLocal<>();
        <T>T executar(Ator ator,Callable<T> trabalho)throws Exception{
            atual.set(ator);try{return trabalho.call();}finally{atual.remove();}
        }
        Connection acompanhar(Connection real)throws SQLException{
            var ator=atual.get();
            if(ator==null)return real;
            ator.conexao=real;
            ator.pid=real.unwrap(org.postgresql.PGConnection.class).getBackendPID();
            ator.conectado.countDown();
            return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(p,m,args)->{
                if(m.getName().equals("commit"))ator.commits.incrementAndGet();
                try{
                    Object resultado=m.invoke(real,args);
                    if(m.getName().equals("prepareStatement") && resultado instanceof PreparedStatement s){
                        boolean dml=((String)args[0]).stripLeading().toLowerCase(Locale.ROOT).matches("(?s)^(insert|update|delete)\\b.*");
                        return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(sp,sm,sa)->{
                            try{Object r=sm.invoke(s,sa);if(dml && sm.getName().startsWith("execute"))ator.dml.incrementAndGet();return r;}
                            catch(InvocationTargetException erro){
                                if(erro.getCause() instanceof SQLException sql)ator.ultimoSqlState=CustodiaPostgresHomologacao19.estadoSeguro(sql);
                                throw erro.getCause();
                            }
                        });
                    }
                    return resultado;
                }
                catch(InvocationTargetException e){
                    if(m.getName().equals("commit") && e.getCause() instanceof SQLException sql)
                        ator.estadoCommit=CustodiaPostgresHomologacao19.estadoSeguro(sql);
                    throw e.getCause();
                }
            });
        }
    }
}
