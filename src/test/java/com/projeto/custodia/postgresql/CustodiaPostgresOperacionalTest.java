package com.projeto.custodia.postgresql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Nenhum adaptador real, porta, processo Docker, contexto ou banco é iniciado. */
class CustodiaPostgresOperacionalTest {
    private final Imagem imagem = new Imagem(IMAGEM, "sha256:" + "a".repeat(64)); // Sintético.
    private final PinPlataforma pin = new PinPlataforma(imagem, "amd64", "linux");
    private final FakeDocker docker = new FakeDocker();
    private final Bootstrap bootstrap = mock(Bootstrap.class);
    private final CustodiaPostgresDataSource.Conector conector = mock(CustodiaPostgresDataSource.Conector.class);
    private final MockEnvironment env = new MockEnvironment();

    private Execucao preparar() { env.setActiveProfiles(CustodiaPostgresSeguranca.PERFIL); return CustodiaPostgresOperacional.preparar(pin, 55437, env, docker, p -> true, bootstrap, conector); }

    private Execucao prepararNoInstante(java.time.Instant inicio) {
        var gerado=Plano.novo(imagem,55437);
        var fixo=new Plano(gerado.execucao(),inicio,imagem,55437,gerado.credenciais());
        try(var planos=mockStatic(Plano.class,CALLS_REAL_METHODS)) {
            planos.when(()->Plano.novo(imagem,55437)).thenReturn(fixo);return preparar();
        }
    }
    @Test void provisionaEConfirmaReciboDeVolumeCriadoNoMesmoSegundo()throws Exception {
        var e=prepararNoInstante(java.time.Instant.parse("2026-10-24T02:41:58.500Z"));
        docker.volumeEmSegundos=true;
        var c=conexao(e.plano(),false);when(conector.abrir(anyString(),anyString(),anyString())).thenReturn(c);
        assertDoesNotThrow(e::provisionar);
        assertEquals(java.time.Instant.parse("2026-10-24T02:41:58Z"),e.recibos().get(Tipo.VOLUME).criadoEm());
        assertTrue(e.pendenciasSemRecibo().isEmpty());e.limpar();assertTrue(e.recibos().isEmpty());
    }
    @Test void duasExecucoesSequenciaisNaMesmaPortaMantemRecibosIndependentes()throws Exception {
        docker.volumeEmSegundos=true;docker.idsPorExecucao=true;
        var primeira=prepararNoInstante(java.time.Instant.parse("2026-10-24T02:41:58.500Z"));
        var c1=conexao(primeira.plano(),false);
        when(conector.abrir(eq(primeira.plano().url()),anyString(),anyString())).thenReturn(c1);
        primeira.provisionar();var recibos1=primeira.recibos();primeira.limpar();
        assertTrue(primeira.recibos().isEmpty());assertTrue(docker.recursos.isEmpty());
        var segunda=prepararNoInstante(java.time.Instant.parse("2026-10-24T02:41:58.800Z"));
        var c2=conexao(segunda.plano(),false);
        when(conector.abrir(eq(segunda.plano().url()),anyString(),anyString())).thenReturn(c2);
        segunda.provisionar();var recibos2=segunda.recibos();
        assertEquals(primeira.plano().porta(),segunda.plano().porta());
        assertNotEquals(primeira.plano().execucao(),segunda.plano().execucao());
        for(var tipo:Tipo.values()) {
            assertNotEquals(recibos1.get(tipo).id(),recibos2.get(tipo).id());
            assertNotEquals(recibos1.get(tipo).rotulos(),recibos2.get(tipo).rotulos());
        }
        segunda.limpar();assertTrue(segunda.recibos().isEmpty());assertTrue(segunda.pendenciasSemRecibo().isEmpty());
        assertTrue(docker.recursos.isEmpty());assertEquals(3,recibos1.size());
        assertEquals(List.of(Tipo.CONTAINER,Tipo.VOLUME,Tipo.REDE,Tipo.CONTAINER,Tipo.VOLUME,Tipo.REDE),docker.removidos);
        verify(c1,times(2)).close();verify(c2,times(2)).close();
    }
    @ParameterizedTest @ValueSource(strings={"VOLUME","CONTAINER","REDE"})
    void provisionamentoRecusaRecursoAnteriorAoLimiteDoSeuTipo(Tipo tipo) {
        var e=prepararNoInstante(java.time.Instant.parse("2026-10-24T02:41:58.500Z"));docker.dataAnterior=tipo;
        assertThrows(Falha.class,e::provisionar);
        assertEquals(1,e.pendenciasSemRecibo().size());assertFalse(e.recibos().containsKey(tipo));
        assertThrows(IllegalStateException.class,e::limpar);
        assertFalse(docker.removidos.contains(tipo));verifyNoInteractions(conector);
    }
    @ParameterizedTest @ValueSource(strings={"nome","id","labels","compose","preexistente"})
    void volumeEmSegundosAindaExigeTodasAsProvasDePropriedade(String divergencia) {
        var e=prepararNoInstante(java.time.Instant.parse("2026-10-24T02:41:58.500Z"));
        docker.volumeEmSegundos=true;docker.volumeDivergente=divergencia;
        assertThrows(Falha.class,e::provisionar);assertTrue(e.recibos().isEmpty());
        assertEquals(1,e.pendenciasSemRecibo().size());assertThrows(IllegalStateException.class,e::limpar);
        assertTrue(docker.removidos.isEmpty());verifyNoInteractions(conector);
    }
    @ParameterizedTest @ValueSource(strings={"timeout","inspecao"})
    void criacaoInconclusivaContinuaSemReciboENuncaEhRemovidaPorNome(String falha) {
        var e=prepararNoInstante(java.time.Instant.parse("2026-10-24T02:41:58.500Z"));docker.volumeEmSegundos=true;
        docker.timeoutVolume=falha.equals("timeout");if(falha.equals("inspecao"))docker.falharInspecao=Tipo.VOLUME;
        assertThrows(Falha.class,e::provisionar);assertTrue(e.recibos().isEmpty());
        assertEquals(1,e.pendenciasSemRecibo().size());assertTrue(docker.recursos.containsKey(Tipo.VOLUME));
        assertEquals(falha.equals("inspecao"),e.pendenciasSemRecibo().get(0).contains(":id="));
        assertThrows(IllegalStateException.class,e::limpar);assertTrue(docker.removidos.isEmpty());
        assertTrue(docker.recursos.containsKey(Tipo.VOLUME));verifyNoInteractions(conector);
    }

    @Test void provisionamentoCompletoSoPublicaDatasourceDepoisDaIdentidadeSql() throws Exception {
        var e = preparar(); assertTrue(docker.chamadas.isEmpty());
        var c = conexao(e.plano(), false); when(conector.abrir(anyString(), anyString(), anyString())).thenReturn(c);
        assertThrows(IllegalStateException.class, e::datasource); e.provisionar();
        assertEquals(3, e.recibos().size()); assertNotNull(e.datasource()); assertEquals(e.plano(), e.manifesto().plano());
        assertEquals(List.of("imagem", "existe:CONTAINER", "existe:VOLUME", "existe:REDE", "criar:VOLUME", "criar:REDE", "criar:CONTAINER", "iniciar", "pronto"), docker.chamadas);
        var ordem = inOrder(bootstrap, conector, c); ordem.verify(bootstrap).preparar(eq(e.manifesto()), any(), eq(e));
        ordem.verify(bootstrap).close(); ordem.verify(conector).abrir(eq(e.plano().url()), eq(e.plano().credenciais().usuario()), eq(e.plano().credenciais().senha()));
        ordem.verify(c).prepareStatement(anyString()); ordem.verify(c).close();
        e.limpar(); assertEquals(List.of(Tipo.CONTAINER, Tipo.VOLUME, Tipo.REDE), docker.removidos);
        assertTrue(e.recibos().isEmpty()); verify(c, times(2)).close();
        assertThrows(IllegalStateException.class, e::datasource); assertThrows(IllegalStateException.class, e::provisionar);
    }

    @ParameterizedTest @ValueSource(strings = {"VOLUME", "REDE", "CONTAINER"})
    void falhaDeCriacaoMantemRecibosParciaisSemApagarNomePresumido(Tipo tipo) throws Exception {
        var e = preparar(); docker.falharCriacao = tipo;
        var falha = assertThrows(Falha.class, e::provisionar); assertSame(e, falha.execucao());
        assertEquals(tipo == Tipo.VOLUME ? 0 : tipo == Tipo.REDE ? 1 : 2, e.recibos().size());
        assertFalse(e.pendenciasSemRecibo().isEmpty()); assertThrows(IllegalStateException.class, e::datasource);
        assertThrows(IllegalStateException.class, e::limpar); // Pendência da tentativa permanece reportada.
        assertFalse(docker.removidos.contains(tipo)); assertTrue(e.recibos().isEmpty()); verifyNoInteractions(conector);
    }

    @ParameterizedTest @ValueSource(strings = {"imagem", "digest", "arquitetura", "sistema", "versao"})
    void imagemOuPlataformaDivergenteImpedeQualquerCriacao(String caso) {
        var e = preparar(); docker.imagemInvalida = caso;
        assertThrows(Falha.class, e::provisionar); assertEquals(List.of("imagem"), docker.chamadas);
        assertTrue(e.recibos().isEmpty()); verifyNoInteractions(conector, bootstrap);
    }

    @Test void portaOcupadaInterrompeAntesDeCriarRecursos() {
        env.setActiveProfiles(CustodiaPostgresSeguranca.PERFIL);
        var e = CustodiaPostgresOperacional.preparar(pin, 55437, env, docker, p -> false, bootstrap, conector);
        assertThrows(Falha.class, e::provisionar); assertEquals(List.of("imagem"), docker.chamadas); verifyNoInteractions(conector, bootstrap);
    }

    @ParameterizedTest @ValueSource(strings = {"VOLUME", "REDE", "CONTAINER"})
    void recursoJaExistenteNaoEReutilizadoOuRemovido(Tipo tipo) {
        var e = preparar(); docker.preexistente = tipo;
        assertThrows(Falha.class, e::provisionar); assertTrue(e.recibos().isEmpty()); assertTrue(docker.removidos.isEmpty());
        assertTrue(docker.chamadas.stream().noneMatch(s -> s.startsWith("criar:"))); verifyNoInteractions(conector, bootstrap);
    }

    @Test void falhaDeInspecaoPreservaIdRecebidoComoPendenciaSemReivindicarPropriedade() {
        var e = preparar(); docker.falharInspecao = Tipo.REDE;
        assertThrows(Falha.class, e::provisionar); assertEquals(1, e.recibos().size());
        assertTrue(e.pendenciasSemRecibo().get(0).contains("id=" + "c".repeat(64)));
        assertTrue(docker.removidos.isEmpty()); verifyNoInteractions(conector, bootstrap);
    }

    @Test void falhaDeConexaoNaoDisponibilizaDatasourceEPermiteLimpezaComRecibos() throws Exception {
        var e = preparar(); when(conector.abrir(anyString(), anyString(), anyString())).thenThrow(new SQLException("sintética"));
        assertThrows(Falha.class, e::provisionar); assertThrows(IllegalStateException.class, e::datasource);
        e.limpar(); assertTrue(e.recibos().isEmpty()); assertEquals(3, docker.removidos.size());
    }

    @Test void identidadeSqlDivergenteFechaConexaoENaoDisponibilizaDatasource() throws Exception {
        var e = preparar(); var c = conexao(e.plano(), true); when(conector.abrir(anyString(), anyString(), anyString())).thenReturn(c);
        assertThrows(Falha.class, e::provisionar); verify(c).close(); assertThrows(IllegalStateException.class, e::datasource);
        e.limpar(); assertEquals(3, docker.removidos.size());
    }

    @Test void falhaDoBootstrapInterrompeAntesDeConectarComoAplicacao() throws Exception {
        var e = preparar(); doThrow(new SQLException("sintética")).when(bootstrap).preparar(any(), any(), any());
        assertThrows(Falha.class, e::provisionar); verifyNoInteractions(conector);
        e.limpar(); assertEquals(3, docker.removidos.size());
    }

    @Test void falhaDeFechamentoImpedeTodaRemocao() throws Exception {
        var e = preparar(); var c = conexao(e.plano(), false); when(conector.abrir(anyString(), anyString(), anyString())).thenReturn(c);
        e.provisionar(); doThrow(new SQLException("fechamento sintético")).when(c).close();
        assertThrows(SQLException.class, e::limpar); assertTrue(docker.removidos.isEmpty()); assertEquals(3, e.recibos().size());
    }

    @Test void falhaAoFecharAdministradorTambemImpedeTodaRemocao() throws Exception {
        var e = preparar(); doThrow(new SQLException("fechamento sintético")).when(bootstrap).close();
        assertThrows(Falha.class, e::provisionar); verifyNoInteractions(conector);
        assertThrows(SQLException.class, e::limpar); assertTrue(docker.removidos.isEmpty());
    }

    @Test void limpezaParcialGuardaRecibosNaoRemovidosEPodeRetomar() throws Exception {
        var e = preparar(); var c = conexao(e.plano(), false); when(conector.abrir(anyString(), anyString(), anyString())).thenReturn(c);
        e.provisionar(); docker.falharRemocao = Tipo.VOLUME;
        assertThrows(IllegalStateException.class, e::limpar); assertEquals(List.of(Tipo.CONTAINER), docker.removidos);
        assertEquals(Set.of(Tipo.VOLUME, Tipo.REDE), e.recibos().keySet());
        docker.falharRemocao = null; e.limpar(); assertTrue(e.recibos().isEmpty());
        assertEquals(List.of(Tipo.CONTAINER, Tipo.VOLUME, Tipo.REDE), docker.removidos);
    }

    @Test void mudancaDePropriedadeRecusaLimpezaAntesDaPrimeiraRemocao() throws Exception {
        var e = preparar(); var c = conexao(e.plano(), false); when(conector.abrir(anyString(), anyString(), anyString())).thenReturn(c);
        e.provisionar(); var volume = docker.recursos.get(Tipo.VOLUME);
        docker.recursos.put(Tipo.VOLUME, new Recurso(Tipo.VOLUME, volume.id(), volume.nome(), volume.criadoEm(), false, Map.of()));
        assertThrows(IllegalStateException.class, e::limpar); assertTrue(docker.removidos.isEmpty());
    }

    @Test void inspecaoRecusaManifestoOuIdDeOutraExecucao() throws Exception {
        var e = preparar(); var c = conexao(e.plano(), false); when(conector.abrir(anyString(), anyString(), anyString())).thenReturn(c); e.provisionar();
        var outroPlano = Plano.novo(imagem, 55438);
        var outro = new CustodiaPostgresManifesto(outroPlano, "d".repeat(64), outroPlano.volume(), "e".repeat(64));
        assertThrows(IllegalStateException.class, () -> e.inspecionar(outro));
        assertThrows(IllegalStateException.class, () -> e.inspecionarRecurso(Tipo.CONTAINER, outro.containerId()));
        assertTrue(docker.removidos.isEmpty());
    }

    @Test void initializerReutilizaExatamenteODatasourceDaExecucaoSemInicializarContexto() throws Exception {
        var e = preparar(); var c = conexao(e.plano(), false); when(conector.abrir(anyString(), anyString(), anyString())).thenReturn(c);
        assertThrows(IllegalStateException.class, e::initializer); e.provisionar();
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        var context = mock(org.springframework.context.ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(env); when(context.getBeanFactory()).thenReturn(factory);
        e.initializer().initialize(context);
        var definicao = (org.springframework.beans.factory.support.RootBeanDefinition) factory.getBeanDefinition("dataSource");
        assertSame(e.datasource(), definicao.getInstanceSupplier().get()); assertFalse(factory.isAllowBeanDefinitionOverriding());
        verify(context, never()).refresh(); e.limpar(); verify(c, times(2)).close();
    }

    private Connection conexao(Plano p, boolean invalida) throws SQLException {
        var c = mock(Connection.class); var s = mock(PreparedStatement.class); var rs = mock(ResultSet.class);
        when(c.prepareStatement(anyString())).thenReturn(s); when(s.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false); when(rs.getString(1)).thenReturn(invalida ? "gestaoacoesdb" : p.banco());
        when(rs.getString(2)).thenReturn(p.credenciais().usuario()); when(rs.getInt(3)).thenReturn(170011);
        when(rs.getString(4)).thenReturn(p.credenciais().marcador()); return c;
    }

    private final class FakeDocker implements Docker {
        final Map<Tipo, Recurso> recursos = new EnumMap<>(Tipo.class);
        final List<String> chamadas = new ArrayList<>(); final List<Tipo> removidos = new ArrayList<>();
        Tipo falharCriacao, preexistente, falharInspecao, falharRemocao; String imagemInvalida; Plano plano;
        boolean volumeEmSegundos,idsPorExecucao,timeoutVolume;Tipo dataAnterior;String volumeDivergente;
        @Override public ImagemLocal imagem(Imagem pin) {
            chamadas.add("imagem"); var img = "imagem".equals(imagemInvalida) || "digest".equals(imagemInvalida) ? new Imagem(IMAGEM, "sha256:" + "d".repeat(64)) : pin;
            return new ImagemLocal(img, "sha256:" + "e".repeat(64), "arquitetura".equals(imagemInvalida) ? "arm64" : "amd64",
                    "sistema".equals(imagemInvalida) ? "windows" : "linux", "versao".equals(imagemInvalida) ? "16.8" : "17.11");
        }
        @Override public boolean existe(Tipo tipo, String nome) { chamadas.add("existe:" + tipo); return tipo == preexistente; }
        private String criar(Tipo t, Plano p) {
            chamadas.add("criar:" + t); if (t == falharCriacao) throw new IllegalStateException("falha sintética"); plano = p;
            String id = t == Tipo.VOLUME ? p.volume() : t == Tipo.REDE ? "c".repeat(64) : "b".repeat(64);
            if(idsPorExecucao && t!=Tipo.VOLUME) {
                String uuid=p.execucao().toString().replace("-","");id=(t==Tipo.REDE?"c":"b")+uuid+uuid.substring(0,31);
            }
            String nome = t == Tipo.VOLUME ? p.volume() : t == Tipo.REDE ? p.rede() : p.container();
            var criado=t==Tipo.VOLUME && volumeEmSegundos ? p.inicio().truncatedTo(java.time.temporal.ChronoUnit.SECONDS) : p.inicio().plusMillis(1);
            if(t==dataAnterior)criado=(t==Tipo.VOLUME?p.inicio().truncatedTo(java.time.temporal.ChronoUnit.SECONDS):p.inicio()).minusNanos(1);
            var labels=new java.util.HashMap<>(p.rotulos());boolean existente=false;String observadoId=id;
            if(t==Tipo.VOLUME) {
                if("nome".equals(volumeDivergente))nome="outro";
                if("id".equals(volumeDivergente))observadoId="outro";
                if("labels".equals(volumeDivergente))labels.clear();
                if("compose".equals(volumeDivergente))labels.put("com.docker.compose.project","dev");
                existente="preexistente".equals(volumeDivergente);
            }
            recursos.put(t, new Recurso(t, observadoId, nome, criado, existente, labels));
            if(t==Tipo.VOLUME && timeoutVolume)throw new IllegalStateException("Timeout simulado após criar volume");
            return id;
        }
        @Override public String criarVolume(Plano p) { return criar(Tipo.VOLUME, p); }
        @Override public String criarRede(Plano p) { return criar(Tipo.REDE, p); }
        @Override public String criarContainer(Plano p, PinPlataforma pin, String v, String r, Administrador a) { return criar(Tipo.CONTAINER, p); }
        @Override public void iniciar(String id) { chamadas.add("iniciar"); }
        @Override public void aguardarPronto(String id, Plano p, Administrador a) { chamadas.add("pronto"); }
        @Override public Recurso inspecionarRecurso(Tipo t, String id) {
            if (t == falharInspecao) throw new IllegalStateException("inspeção sintética"); return recursos.get(t);
        }
        @Override public Estado inspecionar(CustodiaPostgresManifesto m) {
            return new Estado(recursos.get(Tipo.CONTAINER), recursos.get(Tipo.VOLUME), recursos.get(Tipo.REDE), imagem,
                    "127.0.0.1", m.plano().porta(), m.plano().banco(), m.plano().credenciais(), true, m.volumeId(),
                    "/var/lib/postgresql/data", Set.of(m.redeId()), false, false, false);
        }
        @Override public void remover(Recurso r) {
            if (r.tipo() == falharRemocao) throw new IllegalStateException("remoção sintética");
            assertEquals(recursos.get(r.tipo()), r); removidos.add(r.tipo()); recursos.remove(r.tipo());
        }
    }
}
