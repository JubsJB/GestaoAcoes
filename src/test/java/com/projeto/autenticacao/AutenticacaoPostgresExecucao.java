package com.projeto.autenticacao;

import com.projeto.custodia.postgresql.*;
import com.zaxxer.hikari.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.springframework.context.*;
import org.springframework.core.env.*;
import org.springframework.beans.factory.support.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;

/** Reutiliza provisionamento/guardas/descarte existentes. Não aceita URL ou credenciais externas. */
final class AutenticacaoPostgresExecucao implements AutoCloseable {
    static final Path DOCKER = Path.of("C:/Users/julia/AppData/Local/Programs/DockerDesktop/resources/bin/docker.exe");
    static final Imagem PIN = new Imagem(IMAGEM, AutenticacaoPostgresPreflight.DIGEST);
    private static AutenticacaoPostgresExecucao atual;
    final CustodiaPostgresOperacional.Execucao execucao;
    final java.util.List<HikariDataSource> pools=new java.util.concurrent.CopyOnWriteArrayList<>();
    final String jwt = chave(), reset = chave();
    static synchronized AutenticacaoPostgresExecucao obter() {
        if (!"EXECUTAR_009".equals(System.getProperty("auth.pg.real"))) throw new IllegalStateException("Homologação exige -Dauth.pg.real=EXECUTAR_009");
        if (atual == null) atual = new AutenticacaoPostgresExecucao();
        return atual;
    }
    static synchronized void fecharSeAberta() throws Exception { if(atual!=null) { atual.close(); atual=null; } }
    static AutenticacaoPostgresExecucao nova() {
        if(!"EXECUTAR_009".equals(System.getProperty("auth.pg.real")))throw new IllegalStateException("Opt-in obrigatório");
        return new AutenticacaoPostgresExecucao();
    }
    private AutenticacaoPostgresExecucao() {
        String contexto = comando(false,"context","show");
        String endpoint = comando(true,"context","inspect","desktop-linux","--format","{{.Endpoints.docker.Host}}");
        String[] plataforma = comando(true,"version","--format","{{.Server.Os}}|{{.Server.Arch}}").split("\\|");
        var docker = CustodiaPostgresDockerCli.localWindows(DOCKER);
        var imagem = docker.imagem(PIN);
        AutenticacaoPostgresPreflight.validar(contexto,endpoint,plataforma[0],plataforma[1],imagem.imagem().digest());
        var ambiente = new StandardEnvironment(); ambiente.setActiveProfiles(CustodiaPostgresSeguranca.PERFIL);
        int porta;
        try(var socket=new java.net.ServerSocket(0,1,java.net.InetAddress.getLoopbackAddress())) { porta=socket.getLocalPort(); }
        catch(java.io.IOException erro){throw new IllegalStateException("Porta exclusiva indisponível");}
        execucao = preparar(new PinPlataforma(PIN,"amd64","linux"),porta,ambiente,docker,portaLoopback(),CustodiaPostgresJdbc.driverLocal());
        try {
            execucao.provisionar();
            var m=execucao.manifesto();
            var pasta=java.nio.file.Path.of("target/auth-postgresql",m.plano().execucao().toString());java.nio.file.Files.createDirectories(pasta);
            var manifestoSeguro=Map.ofEntries(Map.entry("uuid",m.plano().execucao().toString()),Map.entry("container",m.containerId()),
                    Map.entry("volume",m.volumeId()),Map.entry("rede",m.redeId()),Map.entry("porta",porta),Map.entry("host","127.0.0.1"),
                    Map.entry("banco",m.plano().banco()),Map.entry("imagem",PIN.fixada()),Map.entry("plataforma","linux/amd64"),
                    Map.entry("versao","17.11"),Map.entry("contexto","desktop-linux"),Map.entry("inicio",m.plano().inicio().toString()),Map.entry("rotulos",m.plano().rotulos()));
            java.nio.file.Files.writeString(pasta.resolve("manifesto.json"),new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(manifestoSeguro));
            System.out.println("AUTH_PG_PROVISIONADO uuid="+m.plano().execucao()+" container="+m.containerId()
                    +" volume="+m.volumeId()+" rede="+m.redeId()+" porta="+porta+" versao=17.11 plataforma=linux/amd64 digest="+PIN.digest());
        } catch (Exception erro) {
            try { execucao.limpar(); } catch(Exception limpeza) { System.out.println("AUTH_PG_LIMPEZA_PENDENTE "+execucao+" recibos="+execucao.recibos().keySet()); }
            throw new IllegalStateException("Preflight/provisionamento PostgreSQL recusado antes das migrations");
        }
    }
    static String chave() { byte[] bytes=new byte[64];new java.security.SecureRandom().nextBytes(bytes);return Base64.getEncoder().encodeToString(bytes); }
    static String comando(boolean contexto,String... args) {
        var linha=new ArrayList<String>();linha.add(DOCKER.toString());
        if(contexto) linha.addAll(List.of("--context","desktop-linux"));linha.addAll(List.of(args));
        var builder=new ProcessBuilder(linha).redirectErrorStream(true);
        builder.environment().keySet().removeIf(k->k.toUpperCase(Locale.ROOT).startsWith("DOCKER_"));
        Process p=null;
        try { p=builder.start();if(!p.waitFor(20,TimeUnit.SECONDS)||p.exitValue()!=0)throw new IllegalStateException();
            return new String(p.getInputStream().readNBytes(65536),java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch(Exception e) { throw new IllegalStateException("Comando Docker de preflight/reinício não comprovado"); }
        finally { if(p!=null && p.isAlive())p.destroyForcibly(); }
    }
    DataSource datasource() { return execucao.datasource(); }
    HikariDataSource pool() {
        var cfg=new HikariConfig();cfg.setDataSource(datasource());cfg.setMaximumPoolSize(12);cfg.setMinimumIdle(0);
        cfg.setConnectionTimeout(15000);cfg.setPoolName("auth-pg-"+UUID.randomUUID());
        cfg.setConnectionInitSql("SET lock_timeout='8s'; SET statement_timeout='12s'; SET TIME ZONE 'UTC'");
        var pool=new HikariDataSource(cfg);pools.add(pool);return pool;
    }
    void reiniciarBanco() throws Exception {
        var m=execucao.manifesto();CustodiaPostgresSeguranca.identidade(m,PIN,execucao.inspecionar(m));
        if(pools.stream().anyMatch(p->!p.isClosed()))throw new IllegalStateException("Reinício recusado: pool da aplicação ainda aberto");
        try(var c=datasource().getConnection();var s=c.createStatement()) {
            boolean fechado=false;long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(System.nanoTime()<fim) {
                try(var r=s.executeQuery("select count(*) from pg_stat_activity where datname=current_database() and usename=current_user and pid<>pg_backend_pid()")) {
                    if(r.next()&&r.getInt(1)==0){fechado=true;break;}
                }
                java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
            }
            if(!fechado)throw new IllegalStateException("Reinício recusado: conexões da aplicação ainda abertas");
        }
        comando(true,"container","restart","--time","10",m.containerId());
        long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<fim) {try(var c=datasource().getConnection()){System.out.println("AUTH_PG_REINICIO_BANCO_COMPROVADO");return;}
            catch(java.sql.SQLException e){java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));}}
        throw new IllegalStateException("Banco não retornou após reinício");
    }
    @Override public void close() throws Exception {
        registrar(execucao.plano().execucao().toString(),"CONEXOES_E_RECURSOS","DESCARTE_INICIADO");
        System.out.println("AUTH_PG_EVIDENCIAS_PRESERVADAS_ANTES_DESCARTE uuid="+execucao.plano().execucao());
        execucao.limpar();
        if(!execucao.recibos().isEmpty()||!execucao.pendenciasSemRecibo().isEmpty())throw new IllegalStateException("Descarte incompleto");
        System.out.println("AUTH_PG_DESCARTE_COMPROVADO uuid="+execucao.plano().execucao()+" recursosPendentes=0");
        registrar(execucao.plano().execucao().toString(),"CONEXOES_E_RECURSOS","DESCARTADO");
    }
    static synchronized void registrar(String grupo,String caso,String resultado)throws Exception {
        var pasta=java.nio.file.Path.of("target/auth-postgresql");java.nio.file.Files.createDirectories(pasta);
        String linha=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("grupo",grupo,"caso",caso,"resultado",resultado))+System.lineSeparator();
        java.nio.file.Files.writeString(pasta.resolve("evidencias.jsonl"),linha,java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);
    }
    static void esperarBloqueios(DataSource ds,int quantidade) {
        var jdbc=new JdbcTemplate(ds);long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<fim) {
            var rows=jdbc.queryForList("select pid,pg_blocking_pids(pid) as bloqueadores,wait_event from pg_stat_activity where datname=current_database() and usename=current_user and wait_event_type='Lock' and cardinality(pg_blocking_pids(pid))>0");
            if(rows.size()>=quantidade){System.out.println("AUTH_PG_LOCK_COMPROVADO "+rows);return;}
            java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
        throw new AssertionError("Espera PostgreSQL real por lock não comprovada");
    }
    static void instalarFalha(DataSource ds,String tabela,String evento,String nome) {
        if(!Set.of("usuario","autorizacao_redefinicao_senha","auditoria_usuario","contador_abuso").contains(tabela)
                ||!Set.of("insert","update").contains(evento)||!Set.of("falha_redefinicao","falha_auditoria").contains(nome))throw new IllegalArgumentException();
        var jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE OR REPLACE FUNCTION public.auth_pg_falha() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION USING ERRCODE='45000',MESSAGE='Falha controlada de teste'; END $$");
        jdbc.execute("CREATE TRIGGER "+nome+" BEFORE "+evento+" ON "+tabela+" FOR EACH ROW EXECUTE FUNCTION public.auth_pg_falha()");
    }
    static void removerFalha(DataSource ds,String tabela,String nome) {
        new JdbcTemplate(ds).execute("DROP TRIGGER "+nome+" ON "+tabela);
    }
    public static final class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override public void initialize(ConfigurableApplicationContext ctx) {
            var run=obter();
            ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("auth-pg-exclusivo",Map.ofEntries(
                    Map.entry("auth.jwt.secret",run.jwt),Map.entry("auth.reset.secret",run.reset),
                    Map.entry("spring.jpa.hibernate.ddl-auto","validate"),Map.entry("spring.jpa.show-sql","false"),
                    Map.entry("logging.level.root","WARN"),Map.entry("logging.level.org.hibernate.orm.jdbc.error","OFF"),
                    Map.entry("logging.level.org.hibernate.SQL","OFF"),Map.entry("logging.level.org.hibernate.orm.jdbc.bind","OFF"),
                    Map.entry("spring.main.banner-mode","off"),Map.entry("server.address","127.0.0.1"),Map.entry("server.port","0"),
                    Map.entry("integration.brapi.base-url","http://127.0.0.1:1"),Map.entry("integration.alpha-vantage.base-url","http://127.0.0.1:1"),
                    Map.entry("integration.brasil-api.base-url","http://127.0.0.1:1"),Map.entry("integration.via-cep.base-url","http://127.0.0.1:1"))));
            var factory=(DefaultListableBeanFactory)ctx.getBeanFactory();
            if(factory.containsBeanDefinition("dataSource")||factory.containsSingleton("dataSource"))throw new IllegalStateException("Datasource externo detectado");
            var bean=new RootBeanDefinition(HikariDataSource.class);bean.setInstanceSupplier(run::pool);bean.setDestroyMethodName("close");
            factory.registerBeanDefinition("dataSource",bean);
        }
    }
}
