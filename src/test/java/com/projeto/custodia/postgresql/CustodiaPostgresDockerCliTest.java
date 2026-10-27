package com.projeto.custodia.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresDockerCli.*;
import static org.junit.jupiter.api.Assertions.*;

/** JSON de inspeção sintético e transporte simulado; não cria ProcessBuilder ou sockets. */
class CustodiaPostgresDockerCliTest {
    private final Imagem imagem = new Imagem(IMAGEM, "sha256:" + "a".repeat(64));
    private final PinPlataforma pin = new PinPlataforma(imagem, "amd64", "linux");
    private final Plano aleatorio = Plano.novo(imagem, 55437);
    private final Plano plano = new Plano(aleatorio.execucao(),java.time.Instant.parse("2026-10-24T02:41:58.500Z"),imagem,55437,aleatorio.credenciais());
    private final Administrador admin = Administrador.novo(plano);
    private final Simulador transporte = new Simulador();
    private final CustodiaPostgresDockerCli cli = new CustodiaPostgresDockerCli(transporte);

    @Test void reinspecionaERemoveVolumeRegistradoComCreatedAtEmSegundos() {
        String id=cli.criarVolume(plano);
        var observado=plano.inicio().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        transporte.recursos.get(Tipo.VOLUME).put("CreatedAt",observado.toString());
        var recibo=cli.inspecionarRecurso(Tipo.VOLUME,id);
        assertEquals(observado,recibo.criadoEm());assertFalse(recibo.preexistente());
        assertDoesNotThrow(()->cli.remover(recibo));assertFalse(transporte.recursos.containsKey(Tipo.VOLUME));
        var acoes=transporte.pedidos.stream().map(Pedido::acao).toList();
        assertEquals(List.of(Acao.INSPECIONAR,Acao.REMOVER),acoes.subList(acoes.size()-2,acoes.size()));
    }
    @Test void volumeDoSegundoAnteriorNaoEhRemovidoMesmoComRegistroInterno() {
        String id=cli.criarVolume(plano);
        transporte.recursos.get(Tipo.VOLUME).put("CreatedAt",plano.inicio().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusNanos(1).toString());
        var recibo=cli.inspecionarRecurso(Tipo.VOLUME,id);
        assertThrows(IllegalStateException.class,()->cli.remover(recibo));
        assertTrue(transporte.pedidos.stream().noneMatch(p->p.acao()==Acao.REMOVER));
    }
    @ParameterizedTest @ValueSource(strings={"CONTAINER","REDE"})
    void remocaoMantemPrecisaoTemporalDeContainerERede(Tipo tipo) {
        var m=criar();String id=tipo==Tipo.CONTAINER?m.containerId():m.redeId();
        transporte.recursos.get(tipo).put("Created",plano.inicio().minusNanos(1).toString());
        var recibo=cli.inspecionarRecurso(tipo,id);
        assertThrows(IllegalStateException.class,()->cli.remover(recibo));
        assertTrue(transporte.pedidos.stream().noneMatch(p->p.acao()==Acao.REMOVER || p.acao()==Acao.PARAR));
    }
    @Test void mudancaDeTimestampAposReciboImpedeRemoverVolume() {
        String id=cli.criarVolume(plano);
        var inicio=plano.inicio().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        transporte.recursos.get(Tipo.VOLUME).put("CreatedAt",inicio.toString());
        var recibo=cli.inspecionarRecurso(Tipo.VOLUME,id);
        transporte.recursos.get(Tipo.VOLUME).put("CreatedAt",inicio.plusSeconds(1).toString());
        assertThrows(IllegalStateException.class,()->cli.remover(recibo));
        assertTrue(transporte.pedidos.stream().noneMatch(p->p.acao()==Acao.REMOVER));
    }
    @Test void volumeEmSegundosSemRegistroNesteAdaptadorNaoPodeSerAdotado() {
        String id=cli.criarVolume(plano);
        transporte.recursos.get(Tipo.VOLUME).put("CreatedAt",plano.inicio().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        var outro=new CustodiaPostgresDockerCli(transporte);var recibo=outro.inspecionarRecurso(Tipo.VOLUME,id);
        assertTrue(recibo.preexistente());assertThrows(IllegalStateException.class,()->outro.remover(recibo));
        assertTrue(transporte.pedidos.stream().noneMatch(p->p.acao()==Acao.REMOVER));
    }

    @Test void criaInspecionaERemoveSomenteIdsRegistrados() {
        var m = criar(); var estado = cli.inspecionar(m); CustodiaPostgresSeguranca.identidade(m, imagem, estado);
        assertEquals("sha256:" + "e".repeat(64), cli.imagem(imagem).id());
        var c = cli.inspecionarRecurso(Tipo.CONTAINER, m.containerId()); var v = cli.inspecionarRecurso(Tipo.VOLUME, m.volumeId());
        var r = cli.inspecionarRecurso(Tipo.REDE, m.redeId());
        cli.remover(c); cli.remover(v); cli.remover(r);
        assertTrue(transporte.recursos.isEmpty());
        assertEquals(List.of(m.containerId(), m.volumeId(), m.redeId()), transporte.pedidos.stream()
                .filter(p -> p.acao() == Acao.REMOVER).map(Pedido::alvo).toList());
        assertThrows(IllegalStateException.class, () -> cli.remover(v));
    }

    @Test void argumentosFechadosNaoContemSegredosPullShellOuRemocaoGlobal() {
        criar();
        for (var p : transporte.pedidos) {
            var args = argumentos(p); String texto = args.toString();
            assertFalse(texto.contains(plano.credenciais().senha())); assertFalse(texto.contains(plano.credenciais().marcador()));
            assertFalse(texto.contains(admin.senha()));
            assertTrue(args.stream().noneMatch(s -> List.of("prune", "compose", "pull", "sh", "bash", "cmd", "powershell", "--force", "-f").contains(s)));
            if (p.acao() == Acao.CRIAR_CONTAINER) {
                assertTrue(args.contains("--pull=never")); assertTrue(args.contains(imagem.fixada()));
                assertTrue(args.contains("127.0.0.1:55437:5432")); assertTrue(args.contains("linux/amd64"));
                assertEquals(admin.senha(), envProcesso(p).get("POSTGRES_PASSWORD"));
                assertEquals("scram-sha-256", envProcesso(p).get("POSTGRES_HOST_AUTH_METHOD"));
            }
        }
    }

    @ParameterizedTest @ValueSource(strings = {"digest", "json", "multiplos", "imagemExecutada", "bind", "host", "porta",
            "montagemExtra", "portaExtra", "redeAlheia", "privilegiado", "driverVolume", "rotulo"})
    void inspecaoDivergenteNaoPodeAutorizarConexao(String caso) {
        var m = criar(); transporte.caso = caso;
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresSeguranca.identidade(m, imagem, cli.inspecionar(m)));
    }

    @Test void daemonIndisponivelNaoEquivaleARecursoAusente() {
        transporte.falhaDaemon = true;
        assertThrows(IllegalStateException.class, () -> cli.criarVolume(plano));
        assertTrue(transporte.pedidos.stream().noneMatch(p -> p.acao() == Acao.CRIAR_VOLUME));
    }

    @Test void recursoExistenteNaoEAdotadoPeloRegistroDeCriacao() {
        transporte.registrar(Tipo.VOLUME, plano.volume(), plano);
        assertThrows(IllegalStateException.class, () -> cli.criarVolume(plano));
        var observado = cli.inspecionarRecurso(Tipo.VOLUME, plano.volume()); assertTrue(observado.preexistente());
        assertThrows(IllegalStateException.class, () -> cli.remover(observado));
        assertTrue(transporte.pedidos.stream().noneMatch(p -> p.acao() == Acao.REMOVER));
    }

    @ParameterizedTest @ValueSource(strings = {"postgres", "*", "--all", "a;rm", "../../../postgres_data"})
    void alvoArbitrarioERecusadoAntesDoTransporte(String alvo) {
        assertThrows(IllegalStateException.class, () -> cli.inspecionarRecurso(Tipo.CONTAINER, alvo));
        // existe também deve validar antes de passar qualquer dado ao transporte.
        assertThrows(IllegalStateException.class, () -> argumentos(new Pedido(Acao.INSPECIONAR, Tipo.VOLUME, alvo, null, null, null, null, null)));
        assertTrue(transporte.pedidos.isEmpty());
    }

    @Test void remocaoRecusaMudancaDeRotuloDepoisDoRecibo() {
        var m = criar(); var recibo = cli.inspecionarRecurso(Tipo.VOLUME, m.volumeId()); transporte.caso = "rotulo";
        assertThrows(IllegalStateException.class, () -> cli.remover(recibo));
        assertTrue(transporte.pedidos.stream().noneMatch(p -> p.acao() == Acao.REMOVER));
    }

    @Test void falhaNoStopImpedeRmEReciboContinuaRegistrado() {
        var m = criar(); var recibo = cli.inspecionarRecurso(Tipo.CONTAINER, m.containerId()); transporte.falhaStop = true;
        assertThrows(IllegalStateException.class, () -> cli.remover(recibo));
        assertTrue(transporte.pedidos.stream().noneMatch(p -> p.acao() == Acao.REMOVER));
        transporte.falhaStop = false; cli.remover(recibo);
    }

    @Test void saidaNaoExplicitaSegredosOuDadosDeInspecaoEmToString() {
        assertFalse(new Saida(1, admin.senha()).toString().contains(admin.senha()));
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresDockerCli.localWindows(java.nio.file.Path.of("docker.exe")));
    }

    private CustodiaPostgresManifesto criar() {
        String v = cli.criarVolume(plano); String r = cli.criarRede(plano);
        String c = cli.criarContainer(plano, pin, v, r, admin); cli.iniciar(c); cli.aguardarPronto(c, plano, admin);
        return new CustodiaPostgresManifesto(plano, c, v, r);
    }

    private final class Simulador implements Transporte {
        final ObjectMapper mapper = new ObjectMapper(); final Map<Tipo, ObjectNode> recursos = new EnumMap<>(Tipo.class);
        final List<Pedido> pedidos = new ArrayList<>(); String caso; boolean falhaDaemon, falhaStop;
        @Override public Saida executar(Pedido p) {
            argumentos(p); pedidos.add(p);
            if (p.acao() == Acao.IMAGEM) {
                if ("json".equals(caso)) return new Saida(0, "invalido");
                var n = mapper.createObjectNode(); n.put("Id", "sha256:" + "e".repeat(64)); n.put("Architecture", "amd64"); n.put("Os", "linux");
                n.putArray("RepoDigests").add("postgres@sha256:" + ("digest".equals(caso) ? "d" : "a").repeat(64));
                n.putObject("Config").putArray("Env").add("PG_VERSION=17.11"); return saida(n);
            }
            if (p.acao() == Acao.CRIAR_VOLUME || p.acao() == Acao.CRIAR_REDE || p.acao() == Acao.CRIAR_CONTAINER) {
                String id = p.tipo() == Tipo.VOLUME ? p.plano().volume() : p.tipo() == Tipo.REDE ? "c".repeat(64) : "b".repeat(64);
                registrar(p.tipo(), id, p.plano()); return new Saida(0, id);
            }
            if (p.acao() == Acao.INSPECIONAR) {
                if (falhaDaemon) return new Saida(1, "Cannot connect to daemon");
                var n = recursos.get(p.tipo());
                if (n == null) return new Saida(1, "Error: No such " + (p.tipo() == Tipo.REDE ? "network" : p.tipo() == Tipo.VOLUME ? "volume" : "container") + ": " + p.alvo());
                var copia = n.deepCopy(); adulterar(p.tipo(), copia); return saida(copia);
            }
            if (p.acao() == Acao.PARAR && falhaStop) return new Saida(1, "falha sintética");
            if (p.acao() == Acao.REMOVER) recursos.remove(p.tipo());
            return new Saida(0, "");
        }
        private Saida saida(ObjectNode n) {
            var array = mapper.createArrayNode().add(n); if ("multiplos".equals(caso)) array.add(n);
            return new Saida(0, array.toString());
        }
        void registrar(Tipo t, String id, Plano p) {
            var n = mapper.createObjectNode(); n.put("Id", id); n.put("Name", t == Tipo.VOLUME ? p.volume() : t == Tipo.REDE ? p.rede() : "/" + p.container());
            n.put("Created", p.inicio().plusMillis(1).toString()); n.put("CreatedAt", p.inicio().plusMillis(1).toString());
            var labels = n.putObject("Labels"); p.rotulos().forEach(labels::put);
            n.put("Driver", t == Tipo.REDE ? "bridge" : "local"); n.put("Scope", "local"); n.putObject("Options");
            if (t == Tipo.CONTAINER) {
                var config = n.putObject("Config"); var ls = config.putObject("Labels"); p.rotulos().forEach(ls::put);
                var es = config.putArray("Env"); es.add("POSTGRES_DB=" + p.banco()); es.add("CUSTODIA_TEST_USER=" + p.credenciais().usuario());
                es.add("CUSTODIA_TEST_PASSWORD=" + p.credenciais().senha()); es.add("CUSTODIA_TEST_MARKER=" + p.credenciais().marcador());
                n.put("Image", "sha256:" + "e".repeat(64)); n.putObject("State").put("Running", true);
                var mount = n.putArray("Mounts").addObject(); mount.put("Type", "volume"); mount.put("Name", p.volume()); mount.put("Destination", "/var/lib/postgresql/data");
                var host = n.putObject("HostConfig"); host.put("Privileged", false); host.put("NetworkMode", "c".repeat(64));
                var bind = host.putObject("PortBindings").putArray("5432/tcp").addObject(); bind.put("HostIp", "127.0.0.1"); bind.put("HostPort", "55437");
                var settings = n.putObject("NetworkSettings"); settings.set("Ports", host.path("PortBindings").deepCopy());
                settings.putObject("Networks").putObject(p.rede()).put("NetworkID", "c".repeat(64));
            }
            if (t == Tipo.REDE) n.putObject("Containers").putObject("b".repeat(64));
            recursos.put(t, n);
        }
        void adulterar(Tipo t, ObjectNode n) {
            if (t == Tipo.VOLUME && "driverVolume".equals(caso)) n.put("Driver", "externo");
            if (t == Tipo.VOLUME && "rotulo".equals(caso)) n.putObject("Labels");
            if (t == Tipo.REDE && "redeAlheia".equals(caso)) ((ObjectNode)n.path("Containers")).putObject("d".repeat(64));
            if (t != Tipo.CONTAINER) return;
            if ("imagemExecutada".equals(caso)) n.put("Image", "sha256:" + "f".repeat(64));
            if ("bind".equals(caso)) ((ObjectNode)n.path("Mounts").get(0)).put("Type", "bind");
            if ("montagemExtra".equals(caso)) ((com.fasterxml.jackson.databind.node.ArrayNode)n.path("Mounts")).addObject();
            if ("privilegiado".equals(caso)) ((ObjectNode)n.path("HostConfig")).put("Privileged", true);
            if ("host".equals(caso) || "porta".equals(caso)) {
                var b = (ObjectNode)n.path("HostConfig").path("PortBindings").path("5432/tcp").get(0);
                b.put("host".equals(caso) ? "HostIp" : "HostPort", "host".equals(caso) ? "0.0.0.0" : "5432");
                ((ObjectNode)n.path("NetworkSettings")).set("Ports", n.path("HostConfig").path("PortBindings").deepCopy());
            }
            if ("portaExtra".equals(caso)) ((ObjectNode)n.path("HostConfig").path("PortBindings")).putArray("8080/tcp");
        }
    }
}
