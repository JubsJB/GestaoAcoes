package com.projeto.services;

import com.projeto.entities.LimitePosicaoCustodiaException;
import com.projeto.entities.Mercado;
import com.projeto.entities.PosicaoRacionalCustodia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import static com.projeto.services.EventoQuantitativo.*;
import static com.projeto.services.ValidadorHistoricoQuantitativo.Intencao;
import static com.projeto.services.ValidadorHistoricoQuantitativo.TipoIntencao;
import static com.projeto.services.ValidadorHistoricoQuantitativo.Conflito;

/**
 * Busca pura de uma única candidata, no histórico completo de uma carteira/ação.
 * O chamador valida request, referências e data por mercado e fornece o mesmo snapshot consistente.
 * Não persiste nem altera eventos; cada tentativa usa replay integral e o validador existentes.
 */
public final class BuscadorPosicaoCustodia {
    public record DadosCandidata(long carteiraId, long acaoId, Mercado mercado,
            LocalDate dataEfetiva, BigDecimal quantidade, Grupo origem, Grupo destino) { }

    public enum Codigo {
        SALDO_ORIGEM_INSUFICIENTE, POSICAO_CUSTODIA_INDISPONIVEL,
        LIMITE_POSICAO_CUSTODIA, POSICAO_INSUFICIENTE
    }

    /** A posição identifica a tentativa representativa. Causa é nula apenas no limite de chave. */
    public record Diagnostico(Codigo codigo, PosicaoCronologicaQuantitativa posicao, Conflito causa) {
        public Diagnostico {
            if (codigo == null || posicao == null
                    || (causa == null) != (codigo == Codigo.LIMITE_POSICAO_CUSTODIA)) {
                throw new IllegalArgumentException("Diagnóstico de busca incompleto");
            }
        }
    }

    /** Somente sucesso fornece candidata/posição persistível; uma tentativa inválida não é saída de escrita. */
    public record ResultadoBusca(EventoQuantitativo candidata, PosicaoRacionalCustodia posicaoPersistivel,
            Diagnostico diagnostico, int espacosAvaliados) {
        public ResultadoBusca {
            if (espacosAvaliados <= 0 || (diagnostico == null && (candidata == null || posicaoPersistivel == null))
                    || (diagnostico != null && (candidata != null || posicaoPersistivel != null))) {
                throw new IllegalArgumentException("Resultado de busca inconsistente");
            }
        }
        public boolean encontrada() { return diagnostico == null; }
    }

    public ResultadoBusca buscar(List<EventoQuantitativo> eventosNormalizados, Intencao intencao,
            DadosCandidata dados) {
        if (eventosNormalizados == null || dados == null || intencao == null
                || (intencao.tipo() != TipoIntencao.TRANSFERENCIA && intencao.tipo() != TipoIntencao.ATRIBUICAO)) {
            throw new IllegalArgumentException("Histórico, dados e intenção de custódia são obrigatórios");
        }
        // Validar dados por meio do contrato aprovado antes de iniciar a enumeração.
        EventoQuantitativo modelo = evento(dados, intencao, 0,
                PosicaoRacionalTransitoria.INICIO.entre(PosicaoRacionalTransitoria.FIM));
        List<EventoQuantitativo> historico = new ArrayList<>(eventosNormalizados);
        for (EventoQuantitativo existente : historico) {
            if (existente == null || existente.carteiraId() != modelo.carteiraId()
                    || existente.acaoId() != modelo.acaoId() || existente.mercado() != modelo.mercado()) {
                throw new IllegalArgumentException("Evento nulo ou contexto incompatível com a candidata");
            }
        }
        historico.sort(Comparator.comparing(EventoQuantitativo::posicao));
        var motor = new MotorReplayQuantitativo();
        var validador = new ValidadorHistoricoQuantitativo();
        var anterior = motor.reproduzir(historico);
        validador.verificarPrecondicoes(anterior, intencao); // Inserção já existente é falha de contrato.
        Map<Integer, List<PosicaoRacionalTransitoria>> intervalos = intervalos(historico, dados.dataEfetiva());
        Diagnostico primeiraSemSaldo = null;
        Diagnostico primeiraComSaldo = null;
        int avaliados = 0;
        for (var intervalo : intervalos.entrySet()) {
            var ocupadas = intervalo.getValue();
            var inferior = PosicaoRacionalTransitoria.INICIO;
            for (int i = 0; i <= ocupadas.size(); i++) {
                var superior = i == ocupadas.size() ? PosicaoRacionalTransitoria.FIM : ocupadas.get(i);
                var fracao = inferior.entre(superior);
                EventoQuantitativo candidata = evento(dados, intencao, intervalo.getKey(), fracao);
                List<EventoQuantitativo> simulacao = new ArrayList<>(historico);
                simulacao.add(candidata);
                simulacao.sort(Comparator.comparing(EventoQuantitativo::posicao));
                var posterior = motor.reproduzir(simulacao);
                var validacao = validador.validar(anterior, posterior, intencao);
                avaliados++;
                if (validacao.valido()) {
                    // A capacidade não participa da busca quantitativa nem autoriza pular um espaço válido.
                    try {
                        return new ResultadoBusca(candidata, fracao.paraPersistivel(), null, avaliados);
                    } catch (LimitePosicaoCustodiaException limite) {
                        return falha(new Diagnostico(Codigo.LIMITE_POSICAO_CUSTODIA, candidata.posicao(), null), avaliados);
                    }
                }
                Conflito causa = validacao.conflito();
                switch (causa.codigo()) {
                    case POSICAO_INSUFICIENTE -> {
                        // Custódia não muda o consolidado: histórico financeiro inválido não é espaço descartável.
                        return falha(new Diagnostico(Codigo.POSICAO_INSUFICIENTE, candidata.posicao(), causa), avaliados);
                    }
                    case SALDO_ORIGEM_INSUFICIENTE -> {
                        if (primeiraSemSaldo == null) primeiraSemSaldo = new Diagnostico(
                                Codigo.SALDO_ORIGEM_INSUFICIENTE, candidata.posicao(), causa);
                    }
                    case MOVIMENTACAO_CUSTODIA_INVALIDADA, CONFLITO_HISTORICO_QUANTITATIVO -> {
                        if (primeiraComSaldo == null) primeiraComSaldo = new Diagnostico(
                                Codigo.POSICAO_CUSTODIA_INDISPONIVEL, candidata.posicao(), causa);
                    }
                    default -> throw new IllegalStateException("Diagnóstico incompatível com inserção de custódia");
                }
                inferior = superior;
            }
        }
        return falha(primeiraComSaldo != null ? primeiraComSaldo : primeiraSemSaldo, avaliados);
    }

    private static Map<Integer, List<PosicaoRacionalTransitoria>> intervalos(
            List<EventoQuantitativo> historico, LocalDate data) {
        var intervalos = new TreeMap<Integer, List<PosicaoRacionalTransitoria>>();
        intervalos.put(0, new ArrayList<>());
        for (EventoQuantitativo evento : historico) {
            if (evento.posicao().dataEfetiva().equals(data) && evento.posicao().financeira()) {
                intervalos.put(evento.posicao().ancoraFinanceira(), new ArrayList<>());
            }
        }
        for (EventoQuantitativo evento : historico) {
            if (evento.posicao().dataEfetiva().equals(data) && !evento.posicao().financeira()) {
                var ocupadas = intervalos.get(evento.posicao().ancoraFinanceira());
                if (ocupadas == null) throw new IllegalArgumentException("Âncora não consta das operações financeiras da data");
                // A ordem já é matemática. Original estornada continua ocupando sua chave.
                ocupadas.add(evento.posicao().fracao());
            }
        }
        return intervalos;
    }

    private static EventoQuantitativo evento(DadosCandidata dados, Intencao intencao, int ancora,
            PosicaoRacionalTransitoria fracao) {
        Natureza natureza = intencao.tipo() == TipoIntencao.TRANSFERENCIA
                ? Natureza.TRANSFERENCIA : Natureza.ATRIBUICAO;
        return new EventoQuantitativo(intencao.identidade(), dados.carteiraId(), dados.acaoId(), dados.mercado(),
                natureza, dados.quantidade(), PosicaoCronologicaQuantitativa.custodiaTransitoria(
                dados.dataEfetiva(), ancora, fracao), dados.origem(), dados.destino(), false);
    }
    private static ResultadoBusca falha(Diagnostico diagnostico, int avaliados) {
        return new ResultadoBusca(null, null, diagnostico, avaliados);
    }
}
