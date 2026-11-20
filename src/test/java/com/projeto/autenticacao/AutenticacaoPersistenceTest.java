package com.projeto.autenticacao;

import com.projeto.entities.autenticacao.Papel;
import com.projeto.entities.autenticacao.Usuario;
import com.projeto.repositories.autenticacao.UsuarioRepository;
import com.projeto.entities.autenticacao.*;
import com.projeto.repositories.autenticacao.*;
import com.projeto.dto.UsuarioResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import liquibase.integration.spring.SpringLiquibase;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(AutenticacaoPersistenceTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AutenticacaoPersistenceTest {
    static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-10-30T08:00:00Z");
    @Autowired UsuarioRepository usuarios;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired SessaoAutenticacaoRepository sessoes;
    @Autowired AutorizacaoRedefinicaoSenhaRepository autorizacoes;
    @Autowired AuditoriaUsuarioRepository auditoria;
    @Autowired ContadorAbusoRepository contadores;
    @Autowired ControleSegurancaRepository controle;

    @Test
    @Transactional
    void reidrataSessaoComRevogacaoSemPersistirJwt() {
        Usuario usuario = usuarios.saveAndFlush(new Usuario("sessao.jpa", Papel.USER, AGORA));
        UUID sid = UUID.randomUUID();
        sessoes.saveAndFlush(new SessaoAutenticacao(sid, usuario, 0, AGORA, AGORA.plusMinutes(5),
                AGORA.plusHours(4), AGORA.plusMinutes(6), "LOGOUT"));
        entityManager.clear();
        SessaoAutenticacao registro = sessoes.findByIdForUpdate(sid).orElseThrow();
        assertEquals(sid, registro.getSid());
        assertEquals(usuario.getId(), registro.getUsuario().getId());
        assertEquals(0, registro.getVersaoEmitida());
        assertEquals(0L, registro.getVersao().longValue());
        assertEquals(AGORA.toInstant(), registro.getCriadaEm().toInstant());
        assertEquals(AGORA.plusMinutes(5).toInstant(), registro.getUltimaAtividadeEm().toInstant());
        assertEquals(AGORA.plusHours(4).toInstant(), registro.getExpiraEmAbsoluto().toInstant());
        assertEquals(AGORA.plusMinutes(6).toInstant(), registro.getRevogadaEm().toInstant());
        assertEquals("LOGOUT", registro.getMotivoRevogacao());
    }

    @Test
    @Transactional
    void reidrataVerificadorEAuditoriaSemFluxoDeSenha() {
        Usuario alvo = usuarios.saveAndFlush(new Usuario("titular.jpa", Papel.USER, AGORA));
        Usuario ator = usuarios.saveAndFlush(new Usuario("admin.jpa", Papel.ADMIN, AGORA));
        var original = autorizacoes.saveAndFlush(new AutorizacaoRedefinicaoSenha(UUID.randomUUID(), alvo, ator, "a".repeat(64),
                MotivoRedefinicao.INICIAL, AGORA, AGORA.plusMinutes(15), 5, null, null));
        var evento = auditoria.saveAndFlush(new AuditoriaUsuario(ator, alvo,
                "AUTORIZACAO_REDEFINICAO", "SUCESSO", AGORA));
        entityManager.clear();
        var codigo = autorizacoes.findByIdForUpdate(original.getId()).orElseThrow();
        assertEquals(original.getId(), codigo.getId());
        assertEquals(alvo.getId(), codigo.getUsuario().getId());
        assertEquals(ator.getId(), codigo.getAutorizadaPor().getId());
        assertEquals("a".repeat(64), codigo.getDigest());
        assertEquals(MotivoRedefinicao.INICIAL, codigo.getMotivo());
        assertEquals(AGORA.toInstant(), codigo.getCriadaEm().toInstant());
        assertEquals(AGORA.plusMinutes(15).toInstant(), codigo.getExpiraEm().toInstant());
        assertEquals(5, codigo.getTentativasRestantes());
        assertNull(codigo.getConsumidaEm());
        assertNull(codigo.getInvalidadaEm());
        assertEquals(0L, codigo.getVersao().longValue());
        var auditado = auditoria.findById(evento.getId()).orElseThrow();
        assertEquals(evento.getId(), auditado.getId());
        assertEquals(ator.getId(), auditado.getAtor().getId());
        assertEquals(alvo.getId(), auditado.getAlvo().getId());
        assertEquals("AUTORIZACAO_REDEFINICAO", auditado.getAcao());
        assertEquals("SUCESSO", auditado.getResultado());
        assertEquals(AGORA.toInstant(), auditado.getRegistradaEm().toInstant());
    }

    @Test
    @Transactional
    void reidrataJanelaDeAbusoESingleton() {
        var original = contadores.saveAndFlush(new ContadorAbuso(TipoContadorAbuso.LOGIN_IP,
                "b".repeat(64), AGORA, AGORA.plusMinutes(15), 2));
        entityManager.clear();
        var registro = contadores.findById(original.getId()).orElseThrow();
        assertEquals(original.getId(), registro.getId());
        assertEquals(TipoContadorAbuso.LOGIN_IP, registro.getTipo());
        assertEquals("b".repeat(64), registro.getChaveHash());
        assertEquals(AGORA.toInstant(), registro.getInicioJanela().toInstant());
        assertEquals(AGORA.plusMinutes(15).toInstant(), registro.getExpiraEm().toInstant());
        assertEquals(2, registro.getTentativas());
        assertEquals(0, registro.getVersao());
        assertEquals((short) 1, controle.findByIdForUpdate((short) 1).orElseThrow().getId());
    }

    @Test
    @Transactional
    void respostaPublicaNaoExibeHashPersistido() throws Exception {
        Usuario usuario = usuarios.saveAndFlush(new Usuario("seguro", Papel.ADMIN, AGORA));
        String hashSintetico = "$2b$10$" + "A".repeat(53);
        entityManager.createNativeQuery("UPDATE usuario SET senha_hash=:hash,credencial_definida=true WHERE id=:id")
                .setParameter("hash", hashSintetico).setParameter("id", usuario.getId()).executeUpdate();
        entityManager.clear();
        Usuario persistido = usuarios.findById(usuario.getId()).orElseThrow();
        assertEquals(hashSintetico, persistido.getSenhaHash());
        UsuarioResponse response = UsuarioResponse.from(persistido);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        var json = mapper.readTree(mapper.writeValueAsString(response));
        assertEquals("seguro", json.get("username").asText());
        assertTrue(json.get("credencialDefinida").asBoolean());
        assertEquals(Set.of("id", "username", "papel", "ativo", "credencialDefinida", "criadaEm", "atualizadaEm", "versao"),
                java.util.stream.StreamSupport.stream(
                        java.util.Spliterators.spliteratorUnknownSize(json.fieldNames(), 0), false)
                        .collect(Collectors.toSet()));
        assertFalse(mapper.writeValueAsString(persistido).contains(hashSintetico));
    }

    @Test
    void modeloJpaRepresentaTodasAsEstruturasDePersistenciaAprovadas() {
        assertEquals(Set.of("Usuario", "SessaoAutenticacao", "AutorizacaoRedefinicaoSenha",
                        "ControleSeguranca", "AuditoriaUsuario", "ContadorAbuso"),
                entityManagerFactory.getMetamodel().getEntities().stream()
                        .map(jakarta.persistence.metamodel.EntityType::getName).collect(Collectors.toSet()));
    }

    @Test
    @Transactional
    void salvaEConsultaUsuarioCanonicoSemCredencial() {
        Usuario salvo = usuarios.saveAndFlush(new Usuario("  Aluno.Teste  ", Papel.USER, AGORA));
        entityManager.clear();
        Usuario encontrado = usuarios.findByUsername("aluno.teste").orElseThrow();
        assertEquals(salvo.getId(), encontrado.getId());
        assertEquals("aluno.teste", encontrado.getUsername());
        assertEquals(Papel.USER, encontrado.getPapel());
        assertTrue(encontrado.isAtivo());
        assertFalse(encontrado.isCredencialDefinida());
        assertNull(encontrado.getSenhaHash());
        assertEquals(0, encontrado.getVersaoSeguranca());
        assertEquals(0, encontrado.getVersao());
        assertEquals(AGORA.toInstant(), encontrado.getCriadaEm().toInstant());
        assertEquals(AGORA.toInstant(), encontrado.getAtualizadaEm().toInstant());
    }

    /** Sem Boot/configuração externa, HTTP, providers ou banco persistente. */
    @TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackages = "com.projeto.repositories.autenticacao")
    static class Config {
        @Bean(destroyMethod = "dispose")
        JdbcConnectionPool dataSource() {
            return JdbcConnectionPool.create("jdbc:h2:mem:auth_jpa_" + UUID.randomUUID(), "sa", "");
        }
        @Bean(destroyMethod = "close")
        Connection keeper(JdbcConnectionPool dataSource) throws Exception { return dataSource.getConnection(); }
        @Bean
        @DependsOn("keeper")
        SpringLiquibase liquibase(JdbcConnectionPool dataSource) {
            SpringLiquibase bean = new SpringLiquibase();
            bean.setDataSource(dataSource);
            bean.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
            return bean;
        }
        @Bean
        @DependsOn("liquibase")
        LocalContainerEntityManagerFactoryBean entityManagerFactory(JdbcConnectionPool dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.projeto.entities.autenticacao");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
            return factory;
        }
        @Bean
        JpaTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
    }
}
