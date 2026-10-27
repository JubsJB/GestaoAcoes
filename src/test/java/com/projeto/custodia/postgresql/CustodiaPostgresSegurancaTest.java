package com.projeto.custodia.postgresql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Somente doubles: nenhum Docker, DriverManager, refresh Spring ou banco real. */
class CustodiaPostgresSegurancaTest {
    // Digest sintético para testar contratos; não é pin autorizado de uma imagem real.
    private final Imagem pin = new Imagem(IMAGEM, "sha256:" + "a".repeat(64));
    private final Plano plano = Plano.novo(pin, 55437);
    private final CustodiaPostgresManifesto m = new CustodiaPostgresManifesto(plano, "b".repeat(64), plano.volume(), "c".repeat(64));
    private final Inspecao inspecao = mock(Inspecao.class);
    private final CustodiaPostgresDataSource.Conector conector = mock(CustodiaPostgresDataSource.Conector.class);
    private final MockEnvironment ambiente = ambiente();

    @Test void volumeNoMesmoSegundoDoInicioEhAceitoSemAlterarTimestampDoRecibo() {
        var fixo=new Plano(plano.execucao(),Instant.parse("2026-10-24T02:41:58.500Z"),pin,55437,plano.credenciais());
        var manifesto=new CustodiaPostgresManifesto(fixo,m.containerId(),fixo.volume(),m.redeId());
        var observado=Instant.parse("2026-10-24T02:41:58Z");
        var recibo=new Recurso(Tipo.VOLUME,fixo.volume(),fixo.volume(),observado,false,fixo.rotulos());
        assertDoesNotThrow(()->CustodiaPostgresSeguranca.recurso(manifesto,Tipo.VOLUME,recibo));
        assertEquals(observado,recibo.criadoEm());assertEquals(500_000_000,fixo.inicio().getNano());
    }
    @ParameterizedTest @ValueSource(strings={"2026-10-24T02:41:57Z","2026-10-24T02:41:57.999999999Z"})
    void volumeAnteriorAoSegundoTruncadoContinuaRecusado(String criado) {
        var fixo=new Plano(plano.execucao(),Instant.parse("2026-10-24T02:41:58.500Z"),pin,55437,plano.credenciais());
        var manifesto=new CustodiaPostgresManifesto(fixo,m.containerId(),fixo.volume(),m.redeId());
        var recibo=new Recurso(Tipo.VOLUME,fixo.volume(),fixo.volume(),Instant.parse(criado),false,fixo.rotulos());
        assertThrows(IllegalStateException.class,()->CustodiaPostgresSeguranca.recurso(manifesto,Tipo.VOLUME,recibo));
    }
    @ParameterizedTest @ValueSource(strings={"CONTAINER","REDE"})
    void containerERedeMantemPrecisaoDeNanos(Tipo tipo) {
        var fixo=new Plano(plano.execucao(),Instant.parse("2026-10-24T02:41:58.500Z"),pin,55437,plano.credenciais());
        var manifesto=new CustodiaPostgresManifesto(fixo,m.containerId(),fixo.volume(),m.redeId());
        String id=tipo==Tipo.CONTAINER?m.containerId():m.redeId(),nome=tipo==Tipo.CONTAINER?fixo.container():fixo.rede();
        var antes=new Recurso(tipo,id,nome,fixo.inicio().minusNanos(1),false,fixo.rotulos());
        var exato=new Recurso(tipo,id,nome,fixo.inicio(),false,fixo.rotulos());
        assertThrows(IllegalStateException.class,()->CustodiaPostgresSeguranca.recurso(manifesto,tipo,antes));
        assertDoesNotThrow(()->CustodiaPostgresSeguranca.recurso(manifesto,tipo,exato));
    }
    @ParameterizedTest @ValueSource(strings={"nome","id","preexistente","labels","uuid","compose","semData"})
    void precisaoDeSegundosNaoAutorizaVolumeComPropriedadeDivergente(String caso) {
        var fixo=new Plano(plano.execucao(),Instant.parse("2026-10-24T02:41:58.500Z"),pin,55437,plano.credenciais());
        var manifesto=new CustodiaPostgresManifesto(fixo,m.containerId(),fixo.volume(),m.redeId());
        var labels=new java.util.HashMap<>(fixo.rotulos());
        if(caso.equals("labels"))labels.clear();
        if(caso.equals("uuid"))labels.put(ROTULO,"00000000-0000-0000-0000-000000000000");
        if(caso.equals("compose"))labels.put("com.docker.compose.project","dev");
        var recibo=new Recurso(Tipo.VOLUME,caso.equals("id")?"outro":fixo.volume(),caso.equals("nome")?"outro":fixo.volume(),
                caso.equals("semData")?null:Instant.parse("2026-10-24T02:41:58Z"),caso.equals("preexistente"),labels);
        assertThrows(IllegalStateException.class,()->CustodiaPostgresSeguranca.recurso(manifesto,Tipo.VOLUME,recibo));
    }

    @Test void planoNovoGeraIdentidadesESegredosExclusivosSemProvisionar() {
        Plano outro = Plano.novo(pin, 55438);
        assertNotEquals(plano.execucao(), outro.execucao()); assertNotEquals(plano.volume(), outro.volume());
        assertNotEquals(plano.rede(), outro.rede()); assertNotEquals(plano.container(), outro.container());
        assertNotEquals(plano.banco(), outro.banco()); assertNotEquals(plano.credenciais(), outro.credenciais());
        assertTrue(plano.url().startsWith("jdbc:postgresql://127.0.0.1:55437/custodia_homolog_"));
        assertTrue(pin.fixada().contains("17.11-alpine3.24@sha256:"));
        assertFalse(m.toString().contains(plano.credenciais().senha())); assertFalse(m.toString().contains(plano.credenciais().marcador()));
        CustodiaPostgresSeguranca.identidade(m, pin, estado("valido"));
        ds(); verifyNoInteractions(inspecao, conector);
    }

    @Test void manifestoIncompletoOuCredenciaisDeOutroUuidFalham() {
        assertThrows(IllegalStateException.class, () -> new CustodiaPostgresManifesto(null, m.containerId(), m.volumeId(), m.redeId()));
        assertThrows(IllegalStateException.class, () -> new CustodiaPostgresManifesto(plano, null, m.volumeId(), m.redeId()));
        assertThrows(IllegalStateException.class, () -> new CustodiaPostgresManifesto(plano, m.containerId(), "postgres_data", m.redeId()));
        assertThrows(IllegalStateException.class, () -> new Plano(UUID.randomUUID(), Instant.now(), pin, 55437, plano.credenciais()));
    }

    @ParameterizedTest @ValueSource(ints = {0, 80, 5432, 65536})
    void portaNaoAutorizadaFalha(int porta) { assertThrows(IllegalStateException.class, () -> Plano.novo(pin, porta)); }

    @ParameterizedTest @ValueSource(strings = {"containerId", "containerNome", "uuid", "volumeExistente", "volumeRotulo",
            "rede", "host", "porta", "banco", "usuario", "senha", "digest", "semContainer", "compose",
            "montagem", "destinoVolume", "redeExtra", "portaExtra", "redeHost", "parado", "antigo"})
    void inspecaoIncompativelImpedeConexaoEMigration(String caso) throws Exception {
        when(inspecao.inspecionar(m)).thenReturn(estado(caso));
        var ds = ds(); var migration = mock(Runnable.class);
        assertThrows(IllegalStateException.class, () -> { try (var c = ds.getConnection()) { migration.run(); } });
        verifyNoInteractions(conector, migration);
    }

    @ParameterizedTest @ValueSource(strings = {"jdbc:postgresql://localhost:55437/teste", "jdbc:postgresql://127.0.0.1:5432/gestaoacoesdb",
            "jdbc:postgresql://192.168.0.1:55437/teste", "jdbc:h2:mem:teste", "jdbc:postgresql://127.0.0.1:55437/",
            "invalida", "?currentSchema=public", "?options=-c%20search_path=public", "?user=postgres", ";init=sql"})
    void urlExternaMalformadaOuParametrosJdbcSaoRejeitados(String url) {
        String valor = url.startsWith("?") || url.startsWith(";") ? plano.url() + url : url;
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresSeguranca.destino(m, valor,
                plano.credenciais().usuario(), plano.credenciais().senha())); verifyNoInteractions(conector);
    }

    @Test void credenciaisExternasEDigestDiferenteDoPinSaoRejeitados() throws Exception {
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresSeguranca.destino(m, plano.url(), "postgres", plano.credenciais().senha()));
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresSeguranca.destino(m, plano.url(), plano.credenciais().usuario(), "outra"));
        assertThrows(SQLException.class, () -> ds().getConnection("postgres", "outra"));
        assertThrows(IllegalStateException.class, () -> new CustodiaPostgresDataSource(m,
                new Imagem(IMAGEM, "sha256:" + "d".repeat(64)), inspecao, ambiente, conector));
        assertThrows(IllegalStateException.class, () -> new Imagem("postgres:17-alpine", pin.digest()));
        assertThrows(IllegalStateException.class, () -> new Imagem(IMAGEM, "latest")); verifyNoInteractions(conector);
    }

    @ParameterizedTest @ValueSource(strings = {"spring.datasource.url", "spring.datasource.username", "spring.datasource.password",
            "spring.liquibase.url", "spring.liquibase.enabled", "spring.liquibase.change-log", "spring.jpa.hibernate.ddl-auto",
            "spring.sql.init.mode", "hibernate.connection.url", "jakarta.persistence.jdbc.url", "spring.profiles.active",
            "spring.profiles.include", "spring.application.json", "spring.main.allow-bean-definition-overriding",
            "SPRING_DATASOURCE_URL", "SPRING_CONFIG_IMPORT", "POSTGRES_PASSWORD", "PGSERVICE", "PGOPTIONS", "custodia.pg17.url"})
    void propriedadesExternasSaoRejeitadasInclusiveMascaradas(String chave) {
        ambiente.getPropertySources().addLast(new MapPropertySource("externa", Map.of(chave, "segredo-externo")));
        ambiente.getPropertySources().addFirst(new MapPropertySource("mascara", Map.of(chave, "inofensivo")));
        var falha = assertThrows(IllegalStateException.class, this::ds);
        assertFalse(falha.getMessage().contains("segredo-externo")); verifyNoInteractions(inspecao, conector);
    }

    @ParameterizedTest @ValueSource(strings = {"dev", "test", "custodia-h2", "default"})
    void perfisExternosFalhamAntesDaInspecao(String perfil) {
        ambiente.setActiveProfiles("custodia-pg17", perfil);
        assertThrows(IllegalStateException.class, this::ds); verifyNoInteractions(inspecao, conector);
    }

    @Test void ambienteDesconhecidoEPerfilPadraoExternoFalham() {
        ambiente.setDefaultProfiles("dev"); assertThrows(IllegalStateException.class, this::ds);
        ambiente.setDefaultProfiles("default");
        ambiente.getPropertySources().addFirst(new PropertySource<Object>("opaca") {
            @Override public Object getProperty(String nome) { return null; }
        });
        assertThrows(IllegalStateException.class, this::ds); verifyNoInteractions(conector);
    }

    @Test void revalidaAmbienteAntesDeCadaConexao() {
        var ds = ds(); ambiente.setProperty("spring.liquibase.url", "externa");
        assertThrows(IllegalStateException.class, ds::getConnection); verifyNoInteractions(inspecao, conector);
    }

    @ParameterizedTest @ValueSource(strings = {"banco", "usuario", "versao", "marcador", "ausente", "duplicada"})
    void identidadeSqlInvalidaFechaConexaoAntesDeEntregarAoLiquibase(String caso) throws Exception {
        var c = conexao(caso); when(inspecao.inspecionar(m)).thenReturn(estado("valido"));
        when(conector.abrir(anyString(), anyString(), anyString())).thenReturn(c);
        var migration = mock(Runnable.class); var ds = ds();
        assertThrows(IllegalStateException.class, () -> { try (var ignored = ds.getConnection()) { migration.run(); } });
        verify(c).close(); verifyNoInteractions(migration);
    }

    @Test void conexaoValidaConfereServidorMarcadorEFechaAntesDaRemocao() throws Exception {
        var c = conexao("valida"); when(inspecao.inspecionar(m)).thenReturn(estado("valido"));
        when(conector.abrir(plano.url(), plano.credenciais().usuario(), plano.credenciais().senha())).thenReturn(c);
        var ds = ds(); assertSame(c, ds.getConnection());
        prepararInspecaoRecursos(); var remocao = mock(CustodiaPostgresLimpeza.Remocao.class);
        CustodiaPostgresLimpeza.executar(m, pin, inspecao, ds, remocao);
        var ordem = inOrder(c, remocao); ordem.verify(c).close(); ordem.verify(remocao).container(m.containerId());
        ordem.verify(remocao).volume(m.volumeId()); ordem.verify(remocao).rede(m.redeId());
        verifyNoMoreInteractions(remocao); assertTrue(ds.encerradoSemConexoes());
        assertThrows(IllegalStateException.class, ds::getConnection);
    }

    @ParameterizedTest @ValueSource(strings = {"uuid", "volumeExistente", "compose", "rede", "containerId"})
    void limpezaRecusadaAntesDeQualquerRemocao(String caso) throws Exception {
        when(inspecao.inspecionar(m)).thenReturn(estado(caso)); var remocao = mock(CustodiaPostgresLimpeza.Remocao.class);
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresLimpeza.executar(m, pin, inspecao, ds(), remocao));
        verifyNoInteractions(remocao, conector);
    }

    @Test void divergenciaDepoisDeFecharConexoesTambemImpedeRemocao() throws Exception {
        when(inspecao.inspecionar(m)).thenReturn(estado("valido"), estado("volumeExistente"));
        var remocao = mock(CustodiaPostgresLimpeza.Remocao.class);
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresLimpeza.executar(m, pin, inspecao, ds(), remocao));
        verifyNoInteractions(remocao);
    }

    @Test void reinspecaoDeVolumeImpedeContinuarLimpezaMesmoAposRemoverContainer() throws Exception {
        when(inspecao.inspecionar(m)).thenReturn(estado("valido")); prepararInspecaoRecursos();
        when(inspecao.inspecionarRecurso(Tipo.VOLUME, m.volumeId())).thenReturn(estado("volumeExistente").volume());
        var remocao = mock(CustodiaPostgresLimpeza.Remocao.class);
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresLimpeza.executar(m, pin, inspecao, ds(), remocao));
        verify(remocao).container(m.containerId()); verifyNoMoreInteractions(remocao);
    }

    @Test void falhaDeFechamentoImpedeRemoverRecursos() throws Exception {
        var c = conexao("valida"); when(inspecao.inspecionar(m)).thenReturn(estado("valido"));
        when(conector.abrir(anyString(), anyString(), anyString())).thenReturn(c); doThrow(new SQLException("falha sintética")).when(c).close();
        var ds = ds(); ds.getConnection(); var remocao = mock(CustodiaPostgresLimpeza.Remocao.class);
        assertThrows(SQLException.class, () -> CustodiaPostgresLimpeza.executar(m, pin, inspecao, ds, remocao));
        assertFalse(ds.encerradoSemConexoes()); verifyNoInteractions(remocao);
    }

    @Test void falhaDeIdentidadeEFechamentoNaoPerdeControleDaConexao() throws Exception {
        var c = conexao("versao"); when(inspecao.inspecionar(m)).thenReturn(estado("valido"));
        when(conector.abrir(anyString(), anyString(), anyString())).thenReturn(c);
        doThrow(new SQLException("falha sintética")).when(c).close(); var ds = ds();
        assertThrows(IllegalStateException.class, ds::getConnection); assertThrows(SQLException.class, ds::close);
        assertFalse(ds.encerradoSemConexoes()); verify(c, times(2)).close();
    }

    @Test void initializerBloqueiaSobrescritaSemRefreshOuConexao() {
        when(inspecao.inspecionar(m)).thenReturn(estado("valido"));
        var factory = new DefaultListableBeanFactory(); var context = contexto(factory);
        initializer().initialize(context); assertFalse(factory.isAllowBeanDefinitionOverriding());
        assertEquals(CustodiaPostgresDataSource.class.getName(), factory.getBeanDefinition("dataSource").getBeanClassName());
        assertEquals("close", factory.getBeanDefinition("dataSource").getDestroyMethodName());
        assertThrows(org.springframework.beans.factory.support.BeanDefinitionOverrideException.class,
                () -> factory.registerBeanDefinition("dataSource", new RootBeanDefinition(Object.class)));
        verify(context, never()).refresh(); verifyNoInteractions(conector);
    }

    @ParameterizedTest @ValueSource(strings = {"dataSource", "liquibase", "entityManagerFactory"})
    void initializerRejeitaBeansProtegidosPreexistentes(String nome) {
        when(inspecao.inspecionar(m)).thenReturn(estado("valido")); var factory = new DefaultListableBeanFactory();
        factory.registerBeanDefinition(nome, new RootBeanDefinition(Object.class));
        assertThrows(IllegalStateException.class, () -> initializer().initialize(contexto(factory))); verifyNoInteractions(conector);
    }

    @Test void initializerRecusaDatasourceComOutroNomeEReinspecionaAntesDoRefresh() {
        when(inspecao.inspecionar(m)).thenReturn(estado("valido")); var factory = new DefaultListableBeanFactory();
        factory.registerBeanDefinition("externo", new RootBeanDefinition(org.springframework.jdbc.datasource.DriverManagerDataSource.class));
        assertThrows(IllegalStateException.class, () -> initializer().initialize(contexto(factory))); verifyNoInteractions(conector);
    }

    @Test void configuracoesSomentePreparamBeansESempreCompartilhamDatasource() throws Exception {
        var ds = ds(); var completa = new CustodiaPostgresConfiguracao.Completa();
        var lb = completa.liquibase(ds, new org.springframework.core.io.DefaultResourceLoader());
        assertSame(ds, lb.getDataSource()); assertEquals("classpath:db/changelog/db.changelog-master.yaml", lb.getChangeLog());
        var emf = completa.entityManagerFactory(ds); assertEquals("validate", emf.getJpaPropertyMap().get("hibernate.hbm2ddl.auto"));
        var manual = new CustodiaPostgresConfiguracao.SomenteDatasource().prepararHibernate(ds);
        assertEquals("validate", manual.getJpaPropertyMap().get("hibernate.hbm2ddl.auto"));
        assertTrue(java.util.Arrays.stream(CustodiaPostgresConfiguracao.SomenteDatasource.class.getDeclaredMethods())
                .noneMatch(m -> m.isAnnotationPresent(org.springframework.context.annotation.Bean.class)));
        assertArrayEquals(new String[]{"liquibase"}, CustodiaPostgresConfiguracao.Completa.class
                .getMethod("entityManagerFactory", CustodiaPostgresDataSource.class)
                .getAnnotation(org.springframework.context.annotation.DependsOn.class).value());
        verifyNoInteractions(inspecao, conector);
    }

    @Test void remocaoPossuiSomenteOperacoesTipadasPorIdSemComandoGenerico() {
        assertEquals(Set.of("container", "volume", "rede"), java.util.Arrays.stream(CustodiaPostgresLimpeza.Remocao.class.getMethods())
                .map(java.lang.reflect.Method::getName).collect(java.util.stream.Collectors.toSet()));
        for (var metodo : CustodiaPostgresLimpeza.Remocao.class.getMethods()) assertArrayEquals(new Class<?>[]{String.class}, metodo.getParameterTypes());
    }

    private CustodiaPostgresDataSource ds() { return new CustodiaPostgresDataSource(m, pin, inspecao, ambiente, conector); }
    private CustodiaPostgresInitializer initializer() { return new CustodiaPostgresInitializer(m, pin, inspecao, conector); }
    private ConfigurableApplicationContext contexto(DefaultListableBeanFactory factory) {
        var c = mock(ConfigurableApplicationContext.class); when(c.getEnvironment()).thenReturn(ambiente); when(c.getBeanFactory()).thenReturn(factory); return c;
    }
    private static MockEnvironment ambiente() { var e = new MockEnvironment(); e.setActiveProfiles(CustodiaPostgresSeguranca.PERFIL); return e; }
    private Recurso recurso(Tipo tipo, String id, String nome, boolean existente, Map<String, String> rotulos) {
        return new Recurso(tipo, id, nome, plano.inicio().plusSeconds(1), existente, rotulos);
    }
    private Estado estado(String caso) {
        var rotulos = plano.rotulos();
        if (caso.equals("uuid")) rotulos = Map.of(ROTULO, UUID.randomUUID().toString(), DONO, "homologacao-descartavel");
        if (caso.equals("compose")) { var extras = new java.util.HashMap<>(rotulos); extras.put("com.docker.compose.project", "dev"); rotulos = extras; }
        var container = recurso(Tipo.CONTAINER, caso.equals("containerId") ? "d".repeat(64) : m.containerId(),
                caso.equals("containerNome") ? "postgres-dev" : plano.container(), false, rotulos);
        if (caso.equals("antigo")) container = new Recurso(Tipo.CONTAINER, m.containerId(), plano.container(), plano.inicio().minusSeconds(1), false, rotulos);
        var volume = recurso(Tipo.VOLUME, m.volumeId(), plano.volume(), caso.equals("volumeExistente"), caso.equals("volumeRotulo") ? Map.of() : rotulos);
        var rede = recurso(Tipo.REDE, caso.equals("rede") ? "d".repeat(64) : m.redeId(), plano.rede(), false, rotulos);
        var credenciais = plano.credenciais();
        if (caso.equals("usuario")) credenciais = new Credenciais("custodia_test_" + "d".repeat(32), credenciais.senha(), credenciais.marcador());
        if (caso.equals("senha")) credenciais = new Credenciais(credenciais.usuario(), "d".repeat(64), credenciais.marcador());
        return new Estado(caso.equals("semContainer") ? null : container, volume, rede,
                caso.equals("digest") ? new Imagem(IMAGEM, "sha256:" + "d".repeat(64)) : pin,
                caso.equals("host") ? "0.0.0.0" : "127.0.0.1", caso.equals("porta") ? 5432 : plano.porta(),
                caso.equals("banco") ? "gestaoacoesdb" : plano.banco(), credenciais, !caso.equals("parado"), m.volumeId(),
                caso.equals("destinoVolume") ? "/externo" : "/var/lib/postgresql/data",
                caso.equals("redeExtra") ? Set.of(m.redeId(), "externa") : Set.of(m.redeId()), caso.equals("montagem"),
                caso.equals("portaExtra"), caso.equals("redeHost"));
    }
    private void prepararInspecaoRecursos() {
        Estado e = estado("valido"); when(inspecao.inspecionarRecurso(Tipo.CONTAINER, m.containerId())).thenReturn(e.container());
        when(inspecao.inspecionarRecurso(Tipo.VOLUME, m.volumeId())).thenReturn(e.volume());
        when(inspecao.inspecionarRecurso(Tipo.REDE, m.redeId())).thenReturn(e.rede());
    }
    private Connection conexao(String caso) throws SQLException {
        var c = mock(Connection.class); var s = mock(PreparedStatement.class); var rs = mock(ResultSet.class);
        when(c.prepareStatement(anyString())).thenReturn(s); when(s.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(!caso.equals("ausente"), caso.equals("duplicada"));
        when(rs.getString(1)).thenReturn(caso.equals("banco") ? "gestaoacoesdb" : plano.banco());
        when(rs.getString(2)).thenReturn(caso.equals("usuario") ? "postgres" : plano.credenciais().usuario());
        when(rs.getInt(3)).thenReturn(caso.equals("versao") ? 160008 : 170011);
        when(rs.getString(4)).thenReturn(caso.equals("marcador") ? "outro" : plano.credenciais().marcador()); return c;
    }
}
