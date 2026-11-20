package com.projeto.custodia.postgresql;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/** Somente testes. Recibo de provisionamento futuro, não autorização para executar Docker. */
public record CustodiaPostgresManifesto(Plano plano, String containerId, String volumeId, String redeId) {
    public static final String IMAGEM = "docker.io/library/postgres:17.11-alpine3.24";
    public static final String ROTULO = "com.projeto.custodia.pg17.execucao";
    public static final String DONO = "com.projeto.custodia.pg17.proprietario";

    public CustodiaPostgresManifesto {
        exigir(plano != null, "Plano obrigatório");
        exigir(hexId(containerId) && hexId(redeId), "IDs Docker completos obrigatórios");
        exigir(plano.volume().equals(volumeId), "Identificador do volume deve ser o nome exclusivo registrado");
    }

    /** Sem digest padrão: só aceitar pin conferido/aprovado independentemente do manifesto. */
    public record Imagem(String referencia, String digest) {
        public Imagem {
            exigir(IMAGEM.equals(referencia) && digest != null && digest.matches("sha256:[a-f0-9]{64}"),
                    "Imagem PostgreSQL 17 fixada por versão e digest obrigatória");
        }
        public String fixada() { return referencia + "@" + digest; }
    }

    /** Não imprimir senhas ou marcador, inclusive por toString de registros contenedores. */
    public record Credenciais(String usuario, String senha, String marcador) {
        public Credenciais {
            exigir(usuario != null && usuario.matches("custodia_test_[a-f0-9]{32}"), "Usuário de teste obrigatório");
            exigir(token(senha) && token(marcador) && !senha.equals(marcador), "Segredos independentes obrigatórios");
        }
        @Override public String toString() { return "Credenciais[usuario=" + usuario + ", segredos=omitidos]"; }
    }

    public record Plano(UUID execucao, Instant inicio, Imagem imagem, int porta, Credenciais credenciais) {
        public Plano {
            exigir(execucao != null && inicio != null && imagem != null && credenciais != null, "Plano incompleto");
            exigir(porta >= 1024 && porta <= 65535 && porta != 5432, "Porta exclusiva não padrão obrigatória");
            exigir(credenciais.usuario().equals("custodia_test_" + execucao.toString().replace("-", "")),
                    "Credenciais de outra execução");
        }
        public static Plano novo(Imagem pinAprovado, int portaReservada) {
            UUID id = UUID.randomUUID();
            return new Plano(id, Instant.now(), pinAprovado, portaReservada,
                    new Credenciais("custodia_test_" + id.toString().replace("-", ""), segredo(), segredo()));
        }
        public String container() { return "custodia-pg17-" + execucao; }
        public String volume() { return "custodia-pg17-data-" + execucao; }
        public String rede() { return "custodia-pg17-net-" + execucao; }
        public String banco() { return "custodia_homolog_" + execucao.toString().replace("-", ""); }
        public String url() { return "jdbc:postgresql://127.0.0.1:" + porta + "/" + banco(); }
        public Map<String, String> rotulos() { return Map.of(ROTULO, execucao.toString(), DONO, "homologacao-descartavel"); }
    }

    public enum Tipo { CONTAINER, VOLUME, REDE }
    /** O executor futuro deve produzir estes dados de inspeção real, nunca de valores esperados. */
    public record Recurso(Tipo tipo, String id, String nome, Instant criadoEm,
            boolean preexistente, Map<String, String> rotulos) {
        public Recurso { exigir(rotulos != null, "Rótulos obrigatórios"); rotulos = Map.copyOf(rotulos); }
    }
    /** Digest observado deve vir da imagem resolvida, não apenas da string de criação. */
    public record Estado(Recurso container, Recurso volume, Recurso rede, Imagem imagem,
            String host, int porta, String banco, Credenciais credenciais, boolean executando,
            String volumeMontado, String destinoVolume, java.util.Set<String> redes,
            boolean montagemExtra, boolean portaExtra, boolean redeHost) {
        public Estado { redes = redes == null ? java.util.Set.of() : java.util.Set.copyOf(redes); }
    }
    public interface Inspecao {
        Estado inspecionar(CustodiaPostgresManifesto manifesto);
        Recurso inspecionarRecurso(Tipo tipo, String id);
    }

    private static String segredo() {
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return HexFormat.of().formatHex(bytes);
    }
    private static boolean token(String valor) { return valor != null && valor.matches("[a-f0-9]{64}"); }
    private static boolean hexId(String id) { return id != null && id.matches("[a-f0-9]{64}"); }
    static void exigir(boolean condicao, String mensagem) { if (!condicao) throw new IllegalStateException(mensagem); }
}
