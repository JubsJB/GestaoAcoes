package com.projeto.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.projeto.services.EventoQuantitativo.*;
import static com.projeto.services.MotorReplayQuantitativo.*;

/**
 * Validação pura sobre resultados produzidos pelo motor, sem repetir o replay.
 * O chamador valida request e referências/contexto, carrega fontes completas no mesmo snapshot
 * e monta a simulação correspondente à intenção. Falhas desse contrato são IllegalArgumentException,
 * não diagnósticos HTTP nem comprovação da existência de referências.
 * Para estorno, chamar verificarPrecondicoes ANTES de montar/reproduzir o histórico posterior.
 */
public final class ValidadorHistoricoQuantitativo {
    public enum TipoIntencao { COMPRA, VENDA, TRANSFERENCIA, ATRIBUICAO, ESTORNO }
    public enum Codigo {
        ESTORNO_DUPLICADO, POSICAO_INSUFICIENTE, SALDO_CORRETORA_INSUFICIENTE,
        SALDO_ORIGEM_INSUFICIENTE, MOVIMENTACAO_CUSTODIA_INVALIDADA, CONFLITO_HISTORICO_QUANTITATIVO
    }

    /** Identidade da candidata na inserção; identidade da original no estorno. */
    public record Intencao(TipoIntencao tipo, Identidade identidade) {
        public Intencao {
            if (tipo == null || identidade == null) throw contrato("Intenção e identidade obrigatórias");
            Dominio dominio = tipo == TipoIntencao.COMPRA || tipo == TipoIntencao.VENDA
                    ? Dominio.FINANCEIRO : Dominio.CUSTODIA;
            if (identidade.dominio() != dominio
                    || (tipo == TipoIntencao.ESTORNO && identidade.idPersistido() == null)) {
                throw contrato("Domínio ou identidade incompatível com intenção");
            }
        }
    }

    /** Saldos antes/depois são dos dois históricos no ponto alinhado. Disponível é pré-evento
     * no histórico posterior nas falhas locais; quantidadeNecessaria é nula quando inaplicável. */
    public record Evidencia(BigDecimal saldoAntes, BigDecimal saldoDepois,
            BigDecimal saldoDisponivel, BigDecimal quantidadeNecessaria) {
        public Evidencia {
            Objects.requireNonNull(saldoAntes);
            Objects.requireNonNull(saldoDepois);
        }
        public BigDecimal deficitAntes() { return deficit(saldoAntes); }
        public BigDecimal deficitDepois() { return deficit(saldoDepois); }
    }

    /** Grupo nulo somente para conflito consolidado, que não pertence a uma corretora. */
    public record Conflito(Codigo codigo, Identidade identidade, PosicaoCronologicaQuantitativa posicao,
            Grupo grupo, Evidencia evidencia) {
        public Conflito {
            Objects.requireNonNull(codigo);
            Objects.requireNonNull(identidade);
            Objects.requireNonNull(posicao);
            Objects.requireNonNull(evidencia);
        }
    }
    public record ResultadoValidacao(Conflito conflito) {
        public boolean valido() { return conflito == null; }
    }

    /** Evento identifica o ponto; estados já contêm o transporte onde o ponto é ausente. */
    public record PontoAlinhado(EventoQuantitativo evento, Estado antes, Estado depois) {
        public PontoAlinhado {
            Objects.requireNonNull(evento);
            Objects.requireNonNull(antes);
            Objects.requireNonNull(depois);
        }
    }

    /** Sucesso aqui confirma apenas admissibilidade para simular, não validade quantitativa. */
    public ResultadoValidacao verificarPrecondicoes(Resultado anterior, Intencao intencao) {
        if (intencao == null) throw contrato("Intenção explícita obrigatória");
        Indice antes = indexar(anterior);
        Checkpoint alvo = antes.identidades().get(intencao.identidade());
        if (intencao.tipo() != TipoIntencao.ESTORNO) {
            if (alvo != null) throw contrato("Candidata já existe no histórico anterior");
        } else {
            if (alvo == null) throw contrato("Original alvo não consta do histórico recebido");
            if (alvo.evento().neutralizado()) {
                Grupo grupo = alvo.evento().grupoOrigem();
                BigDecimal saldo = alvo.estado().saldo(grupo);
                return falha(Codigo.ESTORNO_DUPLICADO, alvo.evento(), grupo,
                        new Evidencia(saldo, saldo, null, null));
            }
        }
        return sucesso();
    }

    /** O posterior pode ser nulo apenas quando a precondição já determina estorno duplicado. */
    public ResultadoValidacao validar(Resultado anterior, Resultado posterior, Intencao intencao) {
        ResultadoValidacao precondicao = verificarPrecondicoes(anterior, intencao);
        if (!precondicao.valido()) return precondicao;
        Indice antes = indexar(anterior);
        Indice depois = indexar(posterior);
        conferirCorrespondencia(antes, depois, intencao);
        List<PontoAlinhado> pontos = alinharIndices(antes, depois);
        // Precedência por classe: não retornar déficit antigo antes de procurar conflito prioritário.
        for (PontoAlinhado ponto : pontos) {
            if (ponto.antes().quantidadeConsolidada().signum() < 0
                    || ponto.depois().quantidadeConsolidada().signum() < 0) {
                return falha(Codigo.POSICAO_INSUFICIENTE, ponto.evento(), null,
                        new Evidencia(ponto.antes().quantidadeConsolidada(),
                                ponto.depois().quantidadeConsolidada(), null, null));
            }
        }
        if (intencao.tipo() != TipoIntencao.COMPRA && intencao.tipo() != TipoIntencao.ESTORNO) {
            EventoQuantitativo candidata = depois.identidades().get(intencao.identidade()).evento();
            BigDecimal disponivel = estadoAnterior(depois, candidata.posicao()).saldo(candidata.grupoOrigem());
            if (disponivel.compareTo(candidata.quantidade()) < 0) {
                Codigo codigo = intencao.tipo() == TipoIntencao.VENDA
                        ? Codigo.SALDO_CORRETORA_INSUFICIENTE : Codigo.SALDO_ORIGEM_INSUFICIENTE;
                return conflitoLocal(codigo, candidata, disponivel, pontos);
            }
        }
        for (Checkpoint checkpoint : depois.posicoes().values()) {
            EventoQuantitativo evento = checkpoint.evento();
            if (evento.identidade().dominio() == Dominio.CUSTODIA && !evento.neutralizado()
                    && !evento.identidade().equals(intencao.identidade())) {
                BigDecimal disponivel = estadoAnterior(depois, evento.posicao()).saldo(evento.grupoOrigem());
                if (disponivel.compareTo(evento.quantidade()) < 0) {
                    return conflitoLocal(Codigo.MOVIMENTACAO_CUSTODIA_INVALIDADA, evento, disponivel, pontos);
                }
            }
        }
        return compararPontos(pontos);
    }

    /** Comparação matemática isolada; não substitui validar nem autoriza uma escrita.
     * Permite também pontos exclusivos do histórico anterior, transportando saldo no posterior. */
    public ResultadoValidacao compararDeficits(Resultado anterior, Resultado posterior) {
        return compararPontos(alinhar(anterior, posterior));
    }

    public List<PontoAlinhado> alinhar(Resultado anterior, Resultado posterior) {
        return alinharIndices(indexar(anterior), indexar(posterior));
    }

    private ResultadoValidacao compararPontos(List<PontoAlinhado> pontos) {
        Comparator<Grupo> ordem = Comparator.comparing(Grupo::corretoraId,
                Comparator.nullsLast(Comparator.naturalOrder()));
        for (PontoAlinhado ponto : pontos) {
            var grupos = new TreeSet<>(ordem);
            grupos.addAll(ponto.antes().saldos().keySet());
            grupos.addAll(ponto.depois().saldos().keySet());
            for (Grupo grupo : grupos) {
                Evidencia evidencia = new Evidencia(ponto.antes().saldo(grupo), ponto.depois().saldo(grupo), null, null);
                if (evidencia.deficitDepois().compareTo(evidencia.deficitAntes()) > 0) {
                    return falha(Codigo.CONFLITO_HISTORICO_QUANTITATIVO, ponto.evento(), grupo, evidencia);
                }
            }
        }
        return sucesso();
    }

    private ResultadoValidacao conflitoLocal(Codigo codigo, EventoQuantitativo evento,
            BigDecimal disponivel, List<PontoAlinhado> pontos) {
        PontoAlinhado ponto = pontos.stream().filter(p -> p.evento().identidade().equals(evento.identidade()))
                .findFirst().orElseThrow(() -> contrato("Checkpoint conflitante ausente"));
        Grupo grupo = evento.grupoOrigem();
        return falha(codigo, evento, grupo, new Evidencia(ponto.antes().saldo(grupo),
                ponto.depois().saldo(grupo), disponivel, evento.quantidade()));
    }

    private static void conferirCorrespondencia(Indice antes, Indice depois, Intencao intencao) {
        EventoQuantitativo alvo = depois.identidades().containsKey(intencao.identidade())
                ? depois.identidades().get(intencao.identidade()).evento() : null;
        if (alvo == null) throw contrato("Alvo ausente da simulação posterior");
        boolean estorno = intencao.tipo() == TipoIntencao.ESTORNO;
        if (!estorno && (!alvo.natureza().name().equals(intencao.tipo().name()) || alvo.neutralizado())) {
            throw contrato("Candidata incompatível com intenção");
        }
        int quantidadeEsperada = antes.identidades().size() + (estorno ? 0 : 1);
        if (depois.identidades().size() != quantidadeEsperada) throw contrato("Simulação acrescenta/remove eventos indevidos");
        for (Checkpoint checkpoint : antes.identidades().values()) {
            EventoQuantitativo original = checkpoint.evento();
            EventoQuantitativo esperado = original;
            if (estorno && original.identidade().equals(intencao.identidade())) {
                esperado = new EventoQuantitativo(original.identidade(), original.carteiraId(), original.acaoId(),
                        original.mercado(), original.natureza(), original.quantidade(), original.posicao(),
                        original.grupoOrigem(), original.grupoDestino(), true);
            }
            Checkpoint correspondente = depois.identidades().get(original.identidade());
            if (correspondente == null || !esperado.equals(correspondente.evento())) {
                throw contrato("Simulação altera/remove registro histórico ou não neutraliza a original");
            }
        }
    }

    private record Indice(Map<Identidade, Checkpoint> identidades,
            TreeMap<PosicaoCronologicaQuantitativa, Checkpoint> posicoes) { }

    private static Indice indexar(Resultado resultado) {
        if (resultado == null) throw contrato("Resultado de replay obrigatório");
        Map<Identidade, Checkpoint> ids = new HashMap<>();
        var posicoes = new TreeMap<PosicaoCronologicaQuantitativa, Checkpoint>();
        EventoQuantitativo contexto = null;
        for (Checkpoint checkpoint : resultado.checkpoints()) {
            EventoQuantitativo evento = checkpoint.evento();
            if (contexto != null) conferirContexto(contexto, evento);
            contexto = evento;
            if (ids.putIfAbsent(evento.identidade(), checkpoint) != null
                    || posicoes.putIfAbsent(evento.posicao(), checkpoint) != null) {
                throw contrato("Identidade ou posição duplicada nos checkpoints");
            }
        }
        Estado finalEsperado = posicoes.isEmpty() ? zero() : posicoes.lastEntry().getValue().estado();
        if (!finalEsperado.equals(resultado.estadoFinal())) throw contrato("Estado final incompatível com checkpoints");
        return new Indice(ids, posicoes);
    }

    private static List<PontoAlinhado> alinharIndices(Indice antes, Indice depois) {
        var uniao = new TreeMap<PosicaoCronologicaQuantitativa, EventoQuantitativo>();
        EventoQuantitativo contexto = null;
        for (Indice indice : List.of(antes, depois)) {
            for (Checkpoint checkpoint : indice.posicoes().values()) {
                EventoQuantitativo evento = checkpoint.evento();
                if (contexto != null) conferirContexto(contexto, evento);
                contexto = evento;
                EventoQuantitativo existente = uniao.putIfAbsent(evento.posicao(), evento);
                if (existente != null && !existente.identidade().equals(evento.identidade())) {
                    throw contrato("Colisão entre identidades na mesma posição");
                }
                Checkpoint outro = (indice == antes ? depois : antes).identidades().get(evento.identidade());
                if (outro != null && !outro.evento().posicao().equals(evento.posicao())) {
                    throw contrato("Identidade reposicionada entre históricos");
                }
            }
        }
        List<PontoAlinhado> pontos = new ArrayList<>();
        Estado saldoAntes = zero();
        Estado saldoDepois = zero();
        for (var entrada : uniao.entrySet()) {
            Checkpoint a = antes.posicoes().get(entrada.getKey());
            Checkpoint d = depois.posicoes().get(entrada.getKey());
            if (a != null) saldoAntes = a.estado();
            if (d != null) saldoDepois = d.estado();
            pontos.add(new PontoAlinhado(d != null ? d.evento() : entrada.getValue(), saldoAntes, saldoDepois));
        }
        return List.copyOf(pontos);
    }

    private static Estado estadoAnterior(Indice indice, PosicaoCronologicaQuantitativa posicao) {
        var anterior = indice.posicoes().lowerEntry(posicao);
        return anterior == null ? zero() : anterior.getValue().estado();
    }
    private static void conferirContexto(EventoQuantitativo a, EventoQuantitativo b) {
        if (a.carteiraId() != b.carteiraId() || a.acaoId() != b.acaoId() || a.mercado() != b.mercado()) {
            throw contrato("Contextos incompatíveis");
        }
    }
    private static Estado zero() { return new Estado(Map.of(), BigDecimal.ZERO, Map.of()); }
    private static BigDecimal deficit(BigDecimal saldo) { return saldo.negate().max(BigDecimal.ZERO); }
    private static ResultadoValidacao sucesso() { return new ResultadoValidacao(null); }
    private static ResultadoValidacao falha(Codigo codigo, EventoQuantitativo evento, Grupo grupo, Evidencia evidencia) {
        return new ResultadoValidacao(new Conflito(codigo, evento.identidade(), evento.posicao(), grupo, evidencia));
    }
    private static IllegalArgumentException contrato(String mensagem) { return new IllegalArgumentException(mensagem); }
}
