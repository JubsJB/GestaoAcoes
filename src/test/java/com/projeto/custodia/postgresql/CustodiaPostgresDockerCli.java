package com.projeto.custodia.postgresql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;

/** CLI local Windows com operações fechadas; nenhum método recebe uma linha de comando.
 * Transporte real é opt-in explícito. Construtores e testes não executam processos. */
public final class CustodiaPostgresDockerCli implements Docker {
    enum Acao { INSPECIONAR, IMAGEM, CRIAR_VOLUME, CRIAR_REDE, CRIAR_CONTAINER, INICIAR, PARAR, REMOVER, PRONTO }
    record Pedido(Acao acao, Tipo tipo, String alvo, Plano plano, PinPlataforma pin, String volume,
            String rede, Administrador admin) { }
    record Saida(int codigo, String texto) {
        @Override public String toString() { return "Saida[codigo=" + codigo + ", conteudo=omitido]"; }
    }
    @FunctionalInterface interface Transporte { Saida executar(Pedido pedido); }
    private record Criado(Tipo tipo, Plano plano) { }
    private final Transporte transporte;
    private final Map<String, Criado> criados = new HashMap<>();
    private final ObjectMapper json = new ObjectMapper();

    CustodiaPostgresDockerCli(Transporte transporte) { this.transporte = java.util.Objects.requireNonNull(transporte); }

    /** Nenhum PATH, DOCKER_HOST, DOCKER_CONTEXT ou host remoto é aceito. O caminho do binário
     * deve ser conferido na autorização futura. Imagens nunca são baixadas por este adaptador. */
    public static CustodiaPostgresDockerCli localWindows(Path dockerExe) {
        exigir(dockerExe != null && dockerExe.isAbsolute() && "docker.exe".equalsIgnoreCase(dockerExe.getFileName().toString()),
                "Executável Docker local absoluto obrigatório");
        return new CustodiaPostgresDockerCli(p -> processo(dockerExe, p));
    }

    @Override public ImagemLocal imagem(Imagem pin) {
        var r = objeto(ok(new Pedido(Acao.IMAGEM, null, pin.fixada(), null, null, null, null, null)));
        exigir(r.path("RepoDigests").isArray(), "RepoDigests ausentes");
        boolean encontrado = false;
        for (var digest : r.path("RepoDigests")) {
            encontrado |= Set.of("postgres@" + pin.digest(), "docker.io/library/postgres@" + pin.digest(),
                    "library/postgres@" + pin.digest()).contains(digest.asText());
        }
        exigir(encontrado, "Digest local não comprovado");
        return new ImagemLocal(pin, texto(r, "Id"), texto(r, "Architecture"), texto(r, "Os"), ambiente(r.path("Config")).get("PG_VERSION"));
    }

    @Override public boolean existe(Tipo tipo, String nome) {
        validarAlvo(tipo, nome);
        var saida = transporte.executar(pedido(Acao.INSPECIONAR, tipo, nome));
        if (saida.codigo() == 0) return true;
        // Não transformar daemon indisponível, permissão ou timeout em "recurso ausente".
        String especie = switch (tipo) { case CONTAINER -> "container"; case VOLUME -> "volume"; case REDE -> "network"; };
        String mensagem = saida.texto() == null ? "" : saida.texto().replaceFirst("^\\s*\\[\\]\\s*", "").trim();
        exigir(saida.codigo() == 1 && Set.of("Error: No such " + especie + ": " + nome,
                "Error response from daemon: No such " + especie + ": " + nome,
                "Error response from daemon: get " + nome + ": no such volume",
                "Error response from daemon: network " + nome + " not found").contains(mensagem),
                "Ausência do recurso não comprovada");
        return false;
    }
    @Override public String criarVolume(Plano p) { return criar(Acao.CRIAR_VOLUME, Tipo.VOLUME, p, null, null, null, null); }
    @Override public String criarRede(Plano p) { return criar(Acao.CRIAR_REDE, Tipo.REDE, p, null, null, null, null); }
    @Override public String criarContainer(Plano p, PinPlataforma pin, String volume, String rede, Administrador admin) {
        exigir(p.imagem().equals(pin.imagem()), "Pin divergente");
        registrado(Tipo.VOLUME, volume, p); registrado(Tipo.REDE, rede, p);
        var local = imagem(pin.imagem());
        exigir(pin.arquitetura().equals(local.arquitetura()) && pin.sistema().equals(local.sistema())
                && "17.11".equals(local.versao()), "Plataforma/versão divergente");
        return criar(Acao.CRIAR_CONTAINER, Tipo.CONTAINER, p, pin, volume, rede, admin);
    }
    private String criar(Acao acao, Tipo tipo, Plano p, PinPlataforma pin, String volume, String rede, Administrador admin) {
        String nome = nome(tipo, p); exigir(!existe(tipo, nome), "Criação recusada: recurso existente");
        String id = ok(new Pedido(acao, tipo, nome, p, pin, volume, rede, admin)).trim();
        validarAlvo(tipo, id); exigir(tipo != Tipo.VOLUME || nome.equals(id), "Volume criado divergente");
        criados.put(id, new Criado(tipo, p)); return id;
    }
    @Override public void iniciar(String id) {
        registrado(Tipo.CONTAINER, id, null); ok(pedido(Acao.INICIAR, Tipo.CONTAINER, id));
    }

    /** Readiness somente; não executa SQL ou migrations. Sem retry de escrita. */
    @Override public void aguardarPronto(String id, Plano p, Administrador admin) {
        registrado(Tipo.CONTAINER, id, p);
        for (int i = 0; i < 40; i++) {
            var s = transporte.executar(new Pedido(Acao.PRONTO, Tipo.CONTAINER, id, p, null, null, null, admin));
            if (s.codigo() == 0) return;
            exigir(s.codigo() == 1 || s.codigo() == 2, "Readiness não comprovada");
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Readiness interrompida"); }
        }
        throw new IllegalStateException("PostgreSQL não ficou pronto no prazo");
    }

    @Override public Recurso inspecionarRecurso(Tipo tipo, String id) {
        validarAlvo(tipo, id);
        exigir(tipo == Tipo.VOLUME || id.matches("[a-f0-9]{64}"), "Inspeção exige ID completo");
        var n = objeto(ok(pedido(Acao.INSPECIONAR, tipo, id)));
        String observado = tipo == Tipo.VOLUME ? texto(n, "Name") : texto(n, "Id");
        String nome = texto(n, tipo == Tipo.CONTAINER ? "Name" : "Name"); if (nome.startsWith("/")) nome = nome.substring(1);
        exigir(id.equals(observado), "Inspeção retornou outro ID");
        if (tipo == Tipo.VOLUME) exigir("local".equals(texto(n, "Driver")) && n.path("Options").size() == 0, "Volume com driver/opções externos");
        if (tipo == Tipo.REDE) exigir("bridge".equals(texto(n, "Driver")) && "local".equals(texto(n, "Scope")), "Rede não local");
        Map<String, String> labels = mapa(n.path(tipo == Tipo.CONTAINER ? "Config" : "").path("Labels"));
        if (tipo != Tipo.CONTAINER) labels = mapa(n.path("Labels"));
        Instant criado;
        try { criado = java.time.OffsetDateTime.parse(texto(n, tipo == Tipo.VOLUME ? "CreatedAt" : "Created")).toInstant(); }
        catch (RuntimeException e) { throw new IllegalStateException("Data de criação Docker não comprovada"); }
        return new Recurso(tipo, observado, nome, criado, !criados.containsKey(id), labels);
    }

    @Override public Estado inspecionar(CustodiaPostgresManifesto m) {
        var c = inspecionarRecurso(Tipo.CONTAINER, m.containerId()); var v = inspecionarRecurso(Tipo.VOLUME, m.volumeId());
        var rede = inspecionarRecurso(Tipo.REDE, m.redeId());
        var n = objeto(ok(pedido(Acao.INSPECIONAR, Tipo.CONTAINER, m.containerId())));
        var img = imagem(m.plano().imagem()); exigir(img.id().equals(texto(n, "Image")), "Imagem executada divergente");
        exigir("17.11".equals(img.versao()), "Versão da imagem divergente");
        var env = ambiente(n.path("Config"));
        var cred = new Credenciais(env.get("CUSTODIA_TEST_USER"), env.get("CUSTODIA_TEST_PASSWORD"), env.get("CUSTODIA_TEST_MARKER"));
        var mounts = n.path("Mounts"); exigir(mounts.isArray() && mounts.size() == 1, "Montagens inesperadas");
        var mount = mounts.get(0); exigir("volume".equals(texto(mount, "Type")), "Bind mount proibido");
        var bindings = n.path("HostConfig").path("PortBindings");
        exigir(bindings.isObject() && bindings.size() == 1 && bindings.path("5432/tcp").isArray()
                && bindings.path("5432/tcp").size() == 1, "Portas extras ou ausentes");
        var bind = bindings.path("5432/tcp").get(0);
        var efetivas = n.path("NetworkSettings").path("Ports");
        exigir(efetivas.equals(bindings), "Publicação efetiva de portas divergente");
        var redes = new java.util.HashSet<String>();
        n.path("NetworkSettings").path("Networks").elements().forEachRemaining(r -> redes.add(texto(r, "NetworkID")));
        var redeJson = objeto(ok(pedido(Acao.INSPECIONAR, Tipo.REDE, m.redeId())));
        exigir(redeJson.path("Containers").isObject() && redeJson.path("Containers").size() == 1
                && redeJson.path("Containers").has(m.containerId()), "Rede compartilhada com recurso alheio");
        exigir(!n.path("HostConfig").path("Privileged").asBoolean(true), "Container privilegiado proibido");
        return new Estado(c, v, rede, img.imagem(), texto(bind, "HostIp"), Integer.parseInt(texto(bind, "HostPort")),
                env.get("POSTGRES_DB"), cred, n.path("State").path("Running").asBoolean(false), texto(mount, "Name"),
                texto(mount, "Destination"), redes, false, false, "host".equals(n.path("HostConfig").path("NetworkMode").asText()));
    }

    @Override public void remover(Recurso recibo) {
        exigir(recibo != null, "Recibo obrigatório"); registrado(recibo.tipo(), recibo.id(), null);
        exigir(recibo.equals(inspecionarRecurso(recibo.tipo(), recibo.id())), "Recurso mudou antes da remoção");
        var p = criados.get(recibo.id()).plano();
        exigir(recibo.nome().equals(nome(recibo.tipo(), p)) && !recibo.preexistente()
                && recibo.criadoEm() != null && !recibo.criadoEm().isBefore(CustodiaPostgresSeguranca.inicioComparavel(recibo.tipo(),p))
                && recibo.rotulos().entrySet().containsAll(p.rotulos().entrySet())
                && recibo.rotulos().keySet().stream().noneMatch(k -> k.startsWith("com.docker.compose.")), "Remoção de recurso alheio recusada");
        if (recibo.tipo() == Tipo.CONTAINER) {
            ok(pedido(Acao.PARAR, Tipo.CONTAINER, recibo.id()));
            exigir(recibo.equals(inspecionarRecurso(Tipo.CONTAINER, recibo.id())), "Container mudou após stop");
        }
        ok(pedido(Acao.REMOVER, recibo.tipo(), recibo.id())); criados.remove(recibo.id());
    }
    private void registrado(Tipo tipo, String id, Plano plano) {
        var r = criados.get(id); exigir(r != null && r.tipo() == tipo && (plano == null || r.plano().equals(plano)), "ID não criado/registrado por este adaptador");
    }
    private String ok(Pedido pedido) {
        var s = transporte.executar(pedido); exigir(s != null && s.codigo() == 0, "Operação Docker falhou: " + pedido.acao()); return s.texto();
    }
    private JsonNode objeto(String texto) {
        try { var n = json.readTree(texto); exigir(n.isArray() && n.size() == 1 && n.get(0).isObject(), "Inspeção ambígua"); return n.get(0); }
        catch (java.io.IOException | NullPointerException e) { throw new IllegalStateException("Inspeção Docker inválida"); }
    }
    private static Map<String, String> mapa(JsonNode n) {
        Map<String, String> result = new HashMap<>(); if (n.isNull() || n.isMissingNode()) return result;
        exigir(n.isObject(), "Mapa Docker inválido"); n.fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().asText())); return result;
    }
    private static Map<String, String> ambiente(JsonNode config) {
        exigir(config.path("Env").isArray(), "Ambiente Docker ausente"); Map<String, String> env = new HashMap<>();
        for (var valor : config.path("Env")) { var partes = valor.asText().split("=", 2); exigir(partes.length == 2 && env.putIfAbsent(partes[0], partes[1]) == null, "Ambiente Docker ambíguo"); }
        return env;
    }
    private static String texto(JsonNode n, String campo) { exigir(n.path(campo).isTextual(), "Campo Docker obrigatório: " + campo); return n.path(campo).asText(); }
    private static String nome(Tipo t, Plano p) { return switch (t) { case CONTAINER -> p.container(); case VOLUME -> p.volume(); case REDE -> p.rede(); }; }
    private static Pedido pedido(Acao a, Tipo t, String alvo) { return new Pedido(a, t, alvo, null, null, null, null, null); }
    private static void validarAlvo(Tipo tipo, String alvo) {
        exigir(tipo != null && alvo != null && (alvo.matches("[a-f0-9]{64}") || alvo.matches(
                switch (tipo) { case CONTAINER -> "custodia-pg17-"; case VOLUME -> "custodia-pg17-data-"; case REDE -> "custodia-pg17-net-"; }
                + "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")), "Alvo Docker não autorizado");
    }

    /** Geração fechada de argumentos, também exercitada sem processos pelos testes. */
    static List<String> argumentos(Pedido p) {
        List<String> a = new ArrayList<>();
        if (p.acao() == Acao.IMAGEM) {
            exigir(p.alvo() != null && p.alvo().matches(java.util.regex.Pattern.quote(IMAGEM) + "@sha256:[a-f0-9]{64}"), "Imagem não fixada");
            return List.of("image", "inspect", p.alvo());
        }
        if (Set.of(Acao.INSPECIONAR, Acao.INICIAR, Acao.PARAR, Acao.REMOVER, Acao.PRONTO).contains(p.acao())) {
            validarAlvo(p.tipo(), p.alvo());
            if (p.acao() != Acao.INSPECIONAR) exigir(p.tipo() == Tipo.VOLUME || p.alvo().matches("[a-f0-9]{64}"), "Operação exige ID completo");
            String tipo = switch (p.tipo()) { case CONTAINER -> "container"; case VOLUME -> "volume"; case REDE -> "network"; };
            return switch (p.acao()) {
                case INSPECIONAR -> List.of(tipo, "inspect", p.alvo());
                case INICIAR -> List.of("container", "start", p.alvo());
                case PARAR -> List.of("container", "stop", "--time", "10", p.alvo());
                case REMOVER -> List.of(tipo, "rm", p.alvo());
                case PRONTO -> List.of("container", "exec", p.alvo(), "pg_isready", "-U", p.admin().usuario(), "-d", p.plano().banco());
                default -> throw new IllegalStateException();
            };
        }
        exigir(p.plano() != null, "Plano obrigatório");
        if (p.acao() == Acao.CRIAR_VOLUME || p.acao() == Acao.CRIAR_REDE) {
            a.add(p.acao() == Acao.CRIAR_VOLUME ? "volume" : "network"); a.add("create");
            a.addAll(List.of("--driver", p.acao() == Acao.CRIAR_VOLUME ? "local" : "bridge"));
        } else {
            exigir(p.acao() == Acao.CRIAR_CONTAINER && p.pin() != null && p.admin() != null
                    && p.plano().volume().equals(p.volume()) && p.rede().matches("[a-f0-9]{64}")
                    && p.plano().imagem().equals(p.pin().imagem()), "Criação de container incompleta");
            a.addAll(List.of("container", "create", "--pull=never", "--restart=no", "--name", p.plano().container(),
                    "--platform", p.pin().sistema() + "/" + p.pin().arquitetura(), "--network", p.rede(),
                    "--publish", "127.0.0.1:" + p.plano().porta() + ":5432", "--mount",
                    "type=volume,source=" + p.volume() + ",target=/var/lib/postgresql/data"));
            for (String key : envProcesso(p).keySet().stream().sorted().toList()) a.addAll(List.of("--env", key));
        }
        p.plano().rotulos().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> a.addAll(List.of("--label", e.getKey() + "=" + e.getValue())));
        a.add(p.acao() == Acao.CRIAR_CONTAINER ? p.pin().imagem().fixada() : nome(p.tipo(), p.plano()));
        return List.copyOf(a);
    }
    static Map<String, String> envProcesso(Pedido p) {
        if (p.acao() != Acao.CRIAR_CONTAINER) return Map.of();
        return Map.of("POSTGRES_DB", p.plano().banco(), "POSTGRES_USER", p.admin().usuario(), "POSTGRES_PASSWORD", p.admin().senha(),
                "POSTGRES_HOST_AUTH_METHOD", "scram-sha-256", "CUSTODIA_TEST_USER", p.plano().credenciais().usuario(),
                "CUSTODIA_TEST_PASSWORD", p.plano().credenciais().senha(), "CUSTODIA_TEST_MARKER", p.plano().credenciais().marcador());
    }

    private static Saida processo(Path exe, Pedido pedido) {
        var args = new ArrayList<>(List.of(exe.toString(), "--host", "npipe:////./pipe/dockerDesktopLinuxEngine")); args.addAll(argumentos(pedido));
        var builder = new ProcessBuilder(args).redirectErrorStream(true);
        builder.environment().keySet().removeIf(k -> k.toUpperCase(java.util.Locale.ROOT).startsWith("DOCKER_")); builder.environment().putAll(envProcesso(pedido));
        Process processo = null;
        try {
            processo = builder.start(); final Process iniciado = processo;
            var leitura = new FutureTask<byte[]>(() -> iniciado.getInputStream().readNBytes(1024 * 1024 + 1));
            var thread = new Thread(leitura, "custodia-docker-output"); thread.setDaemon(true); thread.start();
            exigir(processo.waitFor(30, TimeUnit.SECONDS), "Timeout da operação Docker");
            byte[] bytes = leitura.get(2, TimeUnit.SECONDS); exigir(bytes.length <= 1024 * 1024, "Saída Docker excessiva");
            return new Saida(processo.exitValue(), new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Operação Docker interrompida"); }
        catch (Exception e) { throw new IllegalStateException("Falha da operação Docker tipada"); }
        finally { if (processo != null && processo.isAlive()) processo.destroyForcibly(); }
    }
}
