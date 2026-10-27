package com.projeto.custodia.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;

/** Falha fechada; sem DriverManager, comandos Docker, ConfigData ou fallback de URL. */
public final class CustodiaPostgresSeguranca {
    public static final String PERFIL = "custodia-pg17";
    private CustodiaPostgresSeguranca() { }

    /** Somente volumes expõem criação em segundos. Não arredondar o timestamp observado no recibo. */
    static java.time.Instant inicioComparavel(Tipo tipo, Plano plano) {
        return tipo == Tipo.VOLUME ? plano.inicio().truncatedTo(java.time.temporal.ChronoUnit.SECONDS) : plano.inicio();
    }

    public static void ambiente(ConfigurableEnvironment environment) {
        exigir(environment != null && java.util.Arrays.equals(new String[]{PERFIL}, environment.getActiveProfiles()),
                "Somente perfil de homologação PostgreSQL permitido");
        for (var perfil : environment.getDefaultProfiles()) {
            exigir(Set.of("default", PERFIL).contains(perfil), "Perfil padrão proibido");
        }
        for (var source : environment.getPropertySources()) {
            exigir(source instanceof EnumerablePropertySource<?>, "Fonte não enumerável proibida");
            for (var nome : ((EnumerablePropertySource<?>) source).getPropertyNames()) {
                String key = nome.replaceAll("[._-]", "").toLowerCase(Locale.ROOT);
                exigir(!(key.startsWith("springdatasource") || key.startsWith("springliquibase")
                        || key.startsWith("springjpa") || key.startsWith("springsqlinit")
                        || key.startsWith("springprofiles") || key.startsWith("springconfig")
                        || key.startsWith("hibernate") || key.startsWith("jakartapersistence")
                        || key.startsWith("springmain") || key.equals("springapplicationjson")
                        || key.startsWith("custodiapg17") || key.startsWith("postgres") || key.startsWith("pg")),
                        "Propriedade externa protegida proibida");
            }
        }
    }

    public static void identidade(CustodiaPostgresManifesto m, Imagem pinAprovado, Estado e) {
        exigir(m != null && pinAprovado != null && e != null, "Manifesto e inspeção obrigatórios");
        exigir(pinAprovado.equals(m.plano().imagem()) && pinAprovado.equals(e.imagem()), "Imagem/digest divergente");
        recurso(m, Tipo.CONTAINER, e.container()); recurso(m, Tipo.VOLUME, e.volume()); recurso(m, Tipo.REDE, e.rede());
        exigir(e.executando() && "127.0.0.1".equals(e.host()) && m.plano().porta() == e.porta(), "Publicação de porta divergente");
        exigir(m.plano().banco().equals(e.banco()) && m.plano().credenciais().equals(e.credenciais()), "Banco/credenciais divergentes");
        exigir(m.volumeId().equals(e.volumeMontado()) && "/var/lib/postgresql/data".equals(e.destinoVolume())
                && Set.of(m.redeId()).equals(e.redes()) && !e.montagemExtra() && !e.portaExtra() && !e.redeHost(),
                "Montagem, rede ou porta não autorizada");
    }

    public static void recurso(CustodiaPostgresManifesto m, Tipo tipo, Recurso r) {
        exigir(m != null && r != null && tipo == r.tipo(), "Recurso ausente ou tipo divergente");
        String id = switch (tipo) { case CONTAINER -> m.containerId(); case VOLUME -> m.volumeId(); case REDE -> m.redeId(); };
        String nome = switch (tipo) { case CONTAINER -> m.plano().container(); case VOLUME -> m.plano().volume(); case REDE -> m.plano().rede(); };
        exigir(id.equals(r.id()) && nome.equals(r.nome()) && !r.preexistente()
                && r.criadoEm() != null && !r.criadoEm().isBefore(inicioComparavel(tipo,m.plano())), "Propriedade do recurso não comprovada");
        exigir(r.rotulos().entrySet().containsAll(m.plano().rotulos().entrySet())
                && r.rotulos().keySet().stream().noneMatch(k -> k.startsWith("com.docker.compose.")),
                "Rótulos divergentes ou recurso de Compose");
    }

    public static void destino(CustodiaPostgresManifesto m, String url, String usuario, String senha) {
        exigir(m.plano().url().equals(url) && m.plano().credenciais().usuario().equals(usuario)
                && m.plano().credenciais().senha().equals(senha), "Destino ou credenciais não autorizados");
    }

    /** Executar na primeira conexão autorizada, ANTES de entregá-la a Liquibase/Hibernate.
     * O bootstrap futuro cria o marcador em schema separado, sem usar migrations da aplicação. */
    static void sessao(Connection c, CustodiaPostgresManifesto m) throws SQLException {
        try (var statement = c.prepareStatement("SELECT current_database(), current_user, "
                + "current_setting('server_version_num')::integer, marcador "
                + "FROM custodia_pg17_guard.execucao WHERE execucao = ?")) {
            statement.setString(1, m.plano().execucao().toString());
            try (var rs = statement.executeQuery()) {
                exigir(rs.next() && m.plano().banco().equals(rs.getString(1))
                        && m.plano().credenciais().usuario().equals(rs.getString(2))
                        && rs.getInt(3) == 170011 && m.plano().credenciais().marcador().equals(rs.getString(4))
                        && !rs.next(), "Identidade da sessão PostgreSQL não comprovada");
            }
        }
    }
}
