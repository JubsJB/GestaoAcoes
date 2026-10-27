package com.projeto.custodia.postgresql;

import java.time.Duration;
import java.util.concurrent.*;

/** Barreira limitada, independente de JDBC: também exercitada sem infraestrutura. */
final class CustodiaPostgresCoordenacao implements AutoCloseable {
    final CountDownLatch adquirido = new CountDownLatch(1);
    final CountDownLatch liberar = new CountDownLatch(1);
    private final Duration prazo;
    CustodiaPostgresCoordenacao(Duration prazo) { this.prazo = prazo; }
    void aposLock() { adquirido.countDown(); aguardar(liberar); }
    void aguardarLock() { aguardar(adquirido); }
    void aguardar(CountDownLatch sinal) {
        try {
            if (!sinal.await(prazo.toMillis(), TimeUnit.MILLISECONDS)) throw new IllegalStateException("BARREIRA_TIMEOUT");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("BARREIRA_INTERROMPIDA");
        }
    }
    @Override public void close() { liberar.countDown(); }
}
