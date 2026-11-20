package com.projeto.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.projeto.services.EventoQuantitativo.*;

/**
 * Projeção quantitativa pura de um contexto carteira/ação. Recebe a cronologia já normalizada;
 * não ordena, não valida elegibilidade de escritas e não calcula valores financeiros.
 * Cada checkpoint contém o estado posterior ao evento indivisível, inclusive efeito zero.
 */
public final class MotorReplayQuantitativo {
    public record PrimeiroNegativo(Identidade identidade, Natureza natureza,
            PosicaoCronologicaQuantitativa posicao, BigDecimal saldoAposEvento) {
        public PrimeiroNegativo {
            Objects.requireNonNull(identidade);
            Objects.requireNonNull(natureza);
            Objects.requireNonNull(posicao);
            if (saldoAposEvento == null || saldoAposEvento.signum() >= 0) {
                throw new IllegalArgumentException("Evidência exige saldo negativo");
            }
        }
    }

    /**
     * Mantém os grupos encontrados até este ponto, inclusive os zerados e os de originais
     * neutralizadas. Um grupo ainda não encontrado possui saldo zero, acessível por saldo().
     * A evidência é a primeira ocorrência no replay, preservada após regularização e ciclos.
     */
    public record Estado(Map<Grupo, BigDecimal> saldos, BigDecimal quantidadeConsolidada,
            Map<Grupo, PrimeiroNegativo> primeirosNegativos) {
        public Estado {
            saldos = mapaImutavel(saldos);
            Objects.requireNonNull(quantidadeConsolidada);
            primeirosNegativos = mapaImutavel(primeirosNegativos);
        }

        public BigDecimal saldo(Grupo grupo) {
            return saldos.getOrDefault(Objects.requireNonNull(grupo), BigDecimal.ZERO);
        }

        public BigDecimal somaSaldos() {
            return saldos.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        /** Soma atribuída menos consolidado financeiro, sem compensações ou arredondamento. */
        public BigDecimal diferencaReconciliacao() {
            return somaSaldos().subtract(quantidadeConsolidada);
        }

        public boolean reconciliada() { return diferencaReconciliacao().signum() == 0; }
    }

    /** Identidade e posição continuam no evento original imutável, inclusive para candidatas. */
    public record Checkpoint(EventoQuantitativo evento, Estado estado) {
        public Checkpoint {
            Objects.requireNonNull(evento);
            Objects.requireNonNull(estado);
        }
    }

    public record Resultado(List<Checkpoint> checkpoints, Estado estadoFinal) {
        public Resultado {
            checkpoints = List.copyOf(checkpoints);
            Objects.requireNonNull(estadoFinal);
        }
    }

    public Resultado reproduzir(List<EventoQuantitativo> eventosNormalizados) {
        List<EventoQuantitativo> eventos = conferirEntrada(eventosNormalizados);
        Map<Grupo, BigDecimal> saldos = new LinkedHashMap<>();
        Map<Grupo, PrimeiroNegativo> primeiros = new LinkedHashMap<>();
        List<Checkpoint> checkpoints = new ArrayList<>();
        BigDecimal consolidada = BigDecimal.ZERO;
        Estado estado = new Estado(saldos, consolidada, primeiros);
        for (EventoQuantitativo evento : eventos) {
            saldos.putIfAbsent(evento.grupoOrigem(), BigDecimal.ZERO);
            if (evento.grupoDestino() != null) saldos.putIfAbsent(evento.grupoDestino(), BigDecimal.ZERO);
            if (!evento.neutralizado()) {
                switch (evento.natureza()) {
                    case COMPRA -> {
                        variar(saldos, evento.grupoOrigem(), evento.quantidade());
                        consolidada = consolidada.add(evento.quantidade());
                    }
                    case VENDA -> {
                        variar(saldos, evento.grupoOrigem(), evento.quantidade().negate());
                        consolidada = consolidada.subtract(evento.quantidade());
                    }
                    case TRANSFERENCIA, ATRIBUICAO -> {
                        variar(saldos, evento.grupoOrigem(), evento.quantidade().negate());
                        variar(saldos, evento.grupoDestino(), evento.quantidade());
                    }
                }
            }
            // Observar somente após os dois efeitos da custódia; não publicar estado intermediário.
            for (var saldo : saldos.entrySet()) {
                if (saldo.getValue().signum() < 0 && !primeiros.containsKey(saldo.getKey())) {
                    primeiros.put(saldo.getKey(), new PrimeiroNegativo(evento.identidade(), evento.natureza(),
                            evento.posicao(), saldo.getValue()));
                }
            }
            estado = new Estado(saldos, consolidada, primeiros);
            checkpoints.add(new Checkpoint(evento, estado));
        }
        return new Resultado(checkpoints, estado);
    }

    private static void variar(Map<Grupo, BigDecimal> saldos, Grupo grupo, BigDecimal quantidade) {
        saldos.put(grupo, saldos.get(grupo).add(quantidade));
    }

    /** Defesa estrutural: rejeita entrada não normalizada em vez de reordenar ou filtrar. */
    private static List<EventoQuantitativo> conferirEntrada(List<EventoQuantitativo> entrada) {
        if (entrada == null) throw new IllegalArgumentException("Eventos normalizados são obrigatórios");
        List<EventoQuantitativo> eventos = new ArrayList<>(entrada);
        Set<Identidade> identidades = new HashSet<>();
        EventoQuantitativo anterior = null;
        for (EventoQuantitativo evento : eventos) {
            if (evento == null) throw new IllegalArgumentException("Evento nulo");
            if (!identidades.add(evento.identidade())) throw new IllegalArgumentException("Identidade duplicada");
            if (anterior != null && (anterior.posicao().compareTo(evento.posicao()) >= 0
                    || anterior.carteiraId() != evento.carteiraId() || anterior.acaoId() != evento.acaoId()
                    || anterior.mercado() != evento.mercado())) {
                throw new IllegalArgumentException("Entrada não normalizada: cronologia ou contexto incompatível");
            }
            anterior = evento;
        }
        return eventos;
    }

    private static <K, V> Map<K, V> mapaImutavel(Map<K, V> fonte) {
        Map<K, V> copia = new LinkedHashMap<>(fonte);
        copia.forEach((chave, valor) -> {
            Objects.requireNonNull(chave);
            Objects.requireNonNull(valor);
        });
        return Collections.unmodifiableMap(copia);
    }
}
