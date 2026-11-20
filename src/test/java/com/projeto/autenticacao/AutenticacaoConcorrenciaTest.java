package com.projeto.autenticacao;

import com.projeto.entities.autenticacao.Papel;
import com.projeto.entities.autenticacao.Usuario;
import com.projeto.repositories.autenticacao.ControleSegurancaRepository;
import com.projeto.repositories.autenticacao.UsuarioRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(AutenticacaoPersistenceTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AutenticacaoConcorrenciaTest {
    @Autowired UsuarioRepository usuarios;
    @Autowired ControleSegurancaRepository controle;
    @Autowired EntityManagerFactory factory;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void versaoOtimistaRejeitaAtualizacaoObsoleta() {
        Long id = new TransactionTemplate(transactions).execute(status ->
                usuarios.saveAndFlush(new Usuario("concorrente", Papel.USER,
                        AutenticacaoPersistenceTest.AGORA)).getId());
        var primeiro = factory.createEntityManager();
        var segundo = factory.createEntityManager();
        try {
            primeiro.getTransaction().begin();
            segundo.getTransaction().begin();
            var a = primeiro.find(Usuario.class, id);
            var b = segundo.find(Usuario.class, id);
            a.setAtivo(false);
            b.setAtivo(false);
            primeiro.getTransaction().commit();
            RollbackException erro = assertThrows(RollbackException.class, () -> segundo.getTransaction().commit());
            Throwable cause = erro;
            while (cause != null && !(cause instanceof OptimisticLockException)) cause = cause.getCause();
            assertInstanceOf(OptimisticLockException.class, cause);
            assertEquals(1, usuarios.findById(id).orElseThrow().getVersao());
        } finally {
            if (primeiro.getTransaction().isActive()) primeiro.getTransaction().rollback();
            if (segundo.getTransaction().isActive()) segundo.getTransaction().rollback();
            primeiro.close();
            segundo.close();
        }
    }

    @Test
    void singletonSerializaTransacoesIndependentes() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch adquirido = new CountDownLatch(1), liberar = new CountDownLatch(1), iniciouSegundo = new CountDownLatch(1);
        AtomicBoolean primeiroDentro = new AtomicBoolean();
        try {
            Future<?> primeiro = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                controle.findByIdForUpdate((short) 1).orElseThrow();
                primeiroDentro.set(true);
                adquirido.countDown();
                try { assertTrue(liberar.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new AssertionError(ex); }
                primeiroDentro.set(false);
            }));
            assertTrue(adquirido.await(5, TimeUnit.SECONDS));
            Future<?> segundo = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                iniciouSegundo.countDown();
                controle.findByIdForUpdate((short) 1).orElseThrow();
                assertFalse(primeiroDentro.get(), "Lock deve impedir duas entradas simultâneas");
            }));
            assertTrue(iniciouSegundo.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> segundo.get(200, TimeUnit.MILLISECONDS));
            liberar.countDown();
            primeiro.get(5, TimeUnit.SECONDS);
            segundo.get(5, TimeUnit.SECONDS);
        } finally {
            liberar.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
