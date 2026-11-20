package com.projeto.custodia.postgresql;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.ConfigurableEnvironment;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;

/** Preparação operacional opt-in. Construir este objeto não executa Docker ou JDBC. */
public final class CustodiaPostgresOperacional {
    public record PinPlataforma(Imagem imagem, String arquitetura, String sistema) {
        public PinPlataforma {
            exigir(imagem != null && java.util.Set.of("amd64", "arm64").contains(arquitetura)
                    && "linux".equals(sistema), "Pin/plataforma explícitos obrigatórios");
        }
    }
    public record ImagemLocal(Imagem imagem, String id, String arquitetura, String sistema, String versao) { }
    public record Administrador(String usuario, String senha) {
        public Administrador {
            exigir(usuario != null && usuario.matches("custodia_boot_[a-f0-9]{32}")
                    && senha != null && senha.matches("[a-f0-9]{64}"), "Administrador exclusivo obrigatório");
        }
        static Administrador novo(Plano plano) {
            byte[] bytes = new byte[32]; new java.security.SecureRandom().nextBytes(bytes);
            return new Administrador("custodia_boot_" + plano.execucao().toString().replace("-", ""),
                    java.util.HexFormat.of().formatHex(bytes));
        }
        @Override public String toString() { return "Administrador[segredo=omitido]"; }
    }
    /** Nenhuma operação recebe comandos, filtros livres, nomes de Compose ou opções de shell. */
    public interface Docker extends Inspecao {
        ImagemLocal imagem(Imagem pin);
        boolean existe(Tipo tipo, String nome);
        String criarVolume(Plano plano);
        String criarRede(Plano plano);
        String criarContainer(Plano plano, PinPlataforma pin, String volumeId, String redeId, Administrador admin);
        void iniciar(String id);
        void aguardarPronto(String id, Plano plano, Administrador admin);
        void remover(Recurso registrado);
    }
    public interface Bootstrap extends AutoCloseable {
        void preparar(CustodiaPostgresManifesto manifesto, Administrador admin, Inspecao inspecao) throws SQLException;
        @Override void close() throws SQLException;
    }
    @FunctionalInterface public interface Porta { boolean livre(int porta); }

    /** Preparação utilizável no futuro sem ponte circular entre plano e conector. */
    public static Execucao preparar(PinPlataforma pin, int porta, ConfigurableEnvironment ambiente,
            Docker docker, Porta disponibilidade, CustodiaPostgresJdbc jdbc) {
        CustodiaPostgresSeguranca.ambiente(ambiente);
        exigir(pin != null && docker != null && disponibilidade != null && jdbc != null, "Adaptadores explícitos obrigatórios");
        var plano = Plano.novo(pin.imagem(), porta);
        return new Execucao(plano, pin, ambiente, docker, disponibilidade, jdbc, jdbc.conector(plano));
    }

    /** Só sondagem de bind futuro em loopback; nos testes usa-se Porta simulada. */
    public static Porta portaLoopback() {
        return porta -> {
            try (var socket = new java.net.ServerSocket()) {
                socket.setReuseAddress(false);
                socket.bind(new java.net.InetSocketAddress(java.net.InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), porta));
                return true;
            } catch (java.io.IOException e) { return false; }
        };
    }

    public static final class Falha extends IllegalStateException {
        private final Execucao execucao;
        Falha(Execucao e, Exception causa) {
            super("Provisionamento interrompido; consultar recibos e pendências da execução", causa); this.execucao = e;
        }
        public Execucao execucao() { return execucao; }
    }

    public static Execucao preparar(PinPlataforma pin, int porta, ConfigurableEnvironment ambiente,
            Docker docker, Porta disponibilidade, Bootstrap bootstrap, CustodiaPostgresDataSource.Conector conector) {
        CustodiaPostgresSeguranca.ambiente(ambiente);
        exigir(pin != null && docker != null && disponibilidade != null && bootstrap != null && conector != null,
                "Adaptadores explícitos obrigatórios");
        return new Execucao(Plano.novo(pin.imagem(), porta), pin, ambiente, docker, disponibilidade, bootstrap, conector);
    }

    /** Recibos só são removidos do registro após remoção bem-sucedida do recurso exato. */
    public static final class Execucao implements Inspecao {
        private final Plano plano;
        private final PinPlataforma pin;
        private final ConfigurableEnvironment ambiente;
        private final Docker docker;
        private final Porta porta;
        private final Bootstrap bootstrap;
        private final CustodiaPostgresDataSource.Conector conector;
        private final Map<Tipo, Recurso> recibos = new EnumMap<>(Tipo.class);
        private final List<String> incertos = new ArrayList<>();
        private CustodiaPostgresManifesto manifesto;
        private CustodiaPostgresDataSource datasource;
        private boolean iniciada;
        private boolean pronta;
        private boolean encerrada;

        private Execucao(Plano p, PinPlataforma pin, ConfigurableEnvironment e, Docker d, Porta porta,
                Bootstrap bootstrap, CustodiaPostgresDataSource.Conector conector) {
            this.plano = p; this.pin = pin; this.ambiente = e; this.docker = d; this.porta = porta;
            this.bootstrap = bootstrap; this.conector = conector;
        }
        public Plano plano() { return plano; }
        public Map<Tipo, Recurso> recibos() { return Map.copyOf(recibos); }
        public List<String> pendenciasSemRecibo() { return List.copyOf(incertos); }
        public CustodiaPostgresManifesto manifesto() { exigir(pronta && !encerrada, "Execução não disponível"); return manifesto; }
        public CustodiaPostgresDataSource datasource() { exigir(pronta && !encerrada, "Datasource não disponível"); return datasource; }
        public CustodiaPostgresInitializer initializer() {
            exigir(pronta && !encerrada, "Execução não disponível");
            return new CustodiaPostgresInitializer(manifesto, pin.imagem(), this, conector, datasource);
        }

        public synchronized void provisionar() {
            exigir(!iniciada && !encerrada, "Execução não pode ser repetida"); iniciada = true;
            try {
                CustodiaPostgresSeguranca.ambiente(ambiente);
                var local = docker.imagem(pin.imagem());
                exigir(local != null && pin.imagem().equals(local.imagem()) && pin.arquitetura().equals(local.arquitetura())
                        && pin.sistema().equals(local.sistema()) && "17.11".equals(local.versao())
                        && local.id() != null && local.id().matches("sha256:[a-f0-9]{64}"), "Imagem local/plataforma divergente");
                exigir(porta.livre(plano.porta()), "Porta ocupada");
                for (Tipo tipo : Tipo.values()) exigir(!docker.existe(tipo, nome(tipo)), "Recurso já existente");
                criar(Tipo.VOLUME, () -> docker.criarVolume(plano));
                criar(Tipo.REDE, () -> docker.criarRede(plano));
                var admin = Administrador.novo(plano);
                criar(Tipo.CONTAINER, () -> docker.criarContainer(plano, pin, recibos.get(Tipo.VOLUME).id(), recibos.get(Tipo.REDE).id(), admin));
                manifesto = new CustodiaPostgresManifesto(plano, recibos.get(Tipo.CONTAINER).id(),
                        recibos.get(Tipo.VOLUME).id(), recibos.get(Tipo.REDE).id());
                docker.iniciar(manifesto.containerId());
                docker.aguardarPronto(manifesto.containerId(), plano, admin);
                CustodiaPostgresSeguranca.identidade(manifesto, pin.imagem(), inspecionar(manifesto));
                bootstrap.preparar(manifesto, admin, this);
                bootstrap.close(); // Admin não permanece conectado quando se disponibiliza o datasource.
                datasource = new CustodiaPostgresDataSource(manifesto, pin.imagem(), this, ambiente, conector);
                try (var c = datasource.getConnection()) { /* Verificação SQL aprovada antes da publicação. */ }
                pronta = true;
            } catch (Exception falha) { throw new Falha(this, falha); }
        }

        private void criar(Tipo tipo, java.util.function.Supplier<String> criacao) {
            // Uma criação sem resposta conclusiva permanece reportada, nunca é apagada por nome presumido.
            String pendencia = tipo + ":" + nome(tipo); incertos.add(pendencia);
            String id = criacao.get();
            incertos.set(incertos.indexOf(pendencia), pendencia + ":id=" + id);
            var observado = docker.inspecionarRecurso(tipo, id);
            conferirNovo(tipo, id, observado);
            recibos.put(tipo, observado); incertos.remove(pendencia + ":id=" + id);
        }
        private void conferirNovo(Tipo tipo, String id, Recurso r) {
            exigir(r != null && tipo == r.tipo() && id != null && id.equals(r.id()) && nome(tipo).equals(r.nome())
                    && !r.preexistente() && r.criadoEm() != null && !r.criadoEm().isBefore(CustodiaPostgresSeguranca.inicioComparavel(tipo,plano))
                    && r.rotulos().entrySet().containsAll(plano.rotulos().entrySet())
                    && r.rotulos().keySet().stream().noneMatch(k -> k.startsWith("com.docker.compose.")), "Criação exclusiva não comprovada");
            exigir(tipo == Tipo.VOLUME ? plano.volume().equals(id) : id.matches("[a-f0-9]{64}"), "ID de criação inválido");
        }
        private String nome(Tipo tipo) {
            return switch (tipo) { case CONTAINER -> plano.container(); case VOLUME -> plano.volume(); case REDE -> plano.rede(); };
        }
        @Override public Estado inspecionar(CustodiaPostgresManifesto m) {
            exigir(manifesto != null && manifesto.equals(m), "Manifesto alheio");
            for (Tipo tipo : Tipo.values()) conferirRecibo(tipo);
            return docker.inspecionar(m);
        }
        @Override public Recurso inspecionarRecurso(Tipo tipo, String id) {
            exigir(recibos.containsKey(tipo) && recibos.get(tipo).id().equals(id), "Recurso não registrado");
            return conferirRecibo(tipo);
        }
        private Recurso conferirRecibo(Tipo tipo) {
            var recibo = recibos.get(tipo); exigir(recibo != null, "Recibo ausente");
            var atual = docker.inspecionarRecurso(tipo, recibo.id());
            exigir(recibo.equals(atual), "Identidade do recurso mudou desde a criação");
            conferirNovo(tipo, recibo.id(), atual); return atual;
        }

        public synchronized void limpar() throws SQLException {
            encerrada = true; pronta = false;
            bootstrap.close(); if (datasource != null) { datasource.close(); exigir(datasource.encerradoSemConexoes(), "Conexões abertas"); }
            // Conferir todos os recibos antes da primeira remoção e novamente antes de cada remoção.
            for (Tipo tipo : List.copyOf(recibos.keySet())) conferirRecibo(tipo);
            for (Tipo tipo : List.of(Tipo.CONTAINER, Tipo.VOLUME, Tipo.REDE)) {
                if (recibos.containsKey(tipo)) { docker.remover(conferirRecibo(tipo)); recibos.remove(tipo); }
            }
            exigir(incertos.isEmpty(), "Recursos sem recibo exigem investigação; não houve remoção por nome presumido");
        }
        @Override public String toString() { return "Execucao[uuid=" + plano.execucao() + ", recibos=" + recibos.keySet() + "]"; }
    }
    private CustodiaPostgresOperacional() { }
}
