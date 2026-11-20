package com.projeto.services;

import com.projeto.entities.Corretora;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.MovimentacaoCustodia;
import com.projeto.entities.EstornoMovimentacaoCustodia;
import com.projeto.entities.VinculoEstornoConsultado;
import static com.projeto.services.EventoQuantitativo.*;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Projeção pura de fontes completas no snapshot do chamador; sem consultas ou regras de escrita. */
@Component
public final class CalculadoraDistribuicaoCorretora {
    private static final Comparator<Operacao> CRONOLOGIA = Comparator
            .comparing(Operacao::getDataOperacao)
            .thenComparing(Operacao::getOrdemNoDia).thenComparing(Operacao::getId);
    private static final Comparator<DiagnosticoCorretora> ORDEM_GRUPO = Comparator
            .comparing((DiagnosticoCorretora d) -> d.corretoraId() == null)
            .thenComparing(DiagnosticoCorretora::nome)
            .thenComparing(DiagnosticoCorretora::corretoraId, Comparator.nullsLast(Comparator.naturalOrder()));
    private final CalculadoraPosicao consolidado;

    public CalculadoraDistribuicaoCorretora(CalculadoraPosicao consolidado) {
        this.consolidado = Objects.requireNonNull(consolidado);
    }

    public Distribuicao calcular(Long carteiraId, List<Operacao> operacoes) {
        return calcular(carteiraId,operacoes,List.of(),List.of());
    }

    public Distribuicao calcular(Long carteiraId,List<Operacao> operacoes,
            List<MovimentacaoCustodia> movimentos,List<EstornoMovimentacaoCustodia> estornos) {
        if (carteiraId == null || carteiraId <= 0 || operacoes == null || movimentos==null || estornos==null) {
            throw new IllegalArgumentException("Carteira e snapshot de operacoes obrigatorios");
        }
        Map<Long, List<Operacao>> porAcao = new LinkedHashMap<>();
        for (Operacao op : operacoes) {
            if (op == null || op.getCarteira() == null || op.getCarteira().getId() == null) {
                throw historicoInvalido(carteiraId);
            }
            if (!carteiraId.equals(op.getCarteira().getId())) continue;
            if (op.getId() == null || op.getId() <= 0 || op.getAcao() == null
                    || op.getAcao().getId() == null || op.getAcao().getId() <= 0
                    || op.getDataOperacao() == null || op.getOrdemNoDia() == null) {
                throw historicoInvalido(carteiraId);
            }
            porAcao.computeIfAbsent(op.getAcao().getId(), ignored -> new ArrayList<>()).add(op);
        }
        Map<Long,List<MovimentacaoCustodia>> custodiasPorAcao=new LinkedHashMap<>();
        Map<Long,MovimentacaoCustodia> originais=new LinkedHashMap<>();
        for(var m:movimentos){
            if(m==null || !carteiraId.equals(m.getCarteiraId()) || originais.putIfAbsent(m.getId(),m)!=null)
                throw historicoInvalido(carteiraId);
            m.validarEstruturaHistorica();
            custodiasPorAcao.computeIfAbsent(m.getAcaoId(),ignorado->new ArrayList<>()).add(m);
            porAcao.computeIfAbsent(m.getAcaoId(),ignorado->new ArrayList<>());
        }
        Map<Long,EstornoMovimentacaoCustodia> porOriginal=new LinkedHashMap<>();
        for(var e:estornos){
            if(e==null || !originais.containsKey(e.getMovimentacaoOriginalId())
                    || porOriginal.putIfAbsent(e.getMovimentacaoOriginalId(),e)!=null)throw historicoInvalido(carteiraId);
        }
        List<Posicao> posicoes = new ArrayList<>();
        for (var entrada : porAcao.entrySet()) {
            List<Operacao> historico=entrada.getValue();
            var custodias=custodiasPorAcao.getOrDefault(entrada.getKey(),List.of());
            historico.sort(CRONOLOGIA);
            var replay = consolidado.validarQuantidade(historico);
            if (!replay.valido()) throw historicoInvalido(carteiraId);
            BigDecimal quantidade = replay.posicao().quantidadeAtual();
            var vinculos=custodias.stream().map(m->porOriginal.containsKey(m.getId())
                    ?VinculoEstornoConsultado.vinculoPresente(m.getId(),porOriginal.get(m.getId()))
                    :VinculoEstornoConsultado.ausenciaVerificada(m.getId())).toList();
            MotorReplayQuantitativo.Estado estado;
            try{
                var fontes=new AdaptadorEventosQuantitativos().adaptar(carteiraId,entrada.getKey(),historico,custodias,vinculos);
                var eventos=new NormalizadorEventosQuantitativos().normalizar(carteiraId,entrada.getKey(),fontes.financeiros(),fontes.custodias());
                estado=new MotorReplayQuantitativo().reproduzir(eventos).estadoFinal();
            }catch(IllegalArgumentException estruturaInvalida){throw historicoInvalido(carteiraId);}
            if (quantidade.signum() == 0) continue;

            var acao = historico.get(0).getAcao();
            if (acao.getTicker() == null || acao.getTicker().isBlank()
                    || acao.getMoeda() != (acao.getMercado() == Mercado.BRASIL ? Moeda.BRL : Moeda.USD)) {
                throw historicoInvalido(carteiraId);
            }
            Map<Long,String> nomes=new LinkedHashMap<>();
            for (Operacao op : historico) {
                Corretora corretora = op.getCorretora();
                Long id = corretora == null ? null : corretora.getId();
                if (corretora != null && (id == null || id <= 0)) throw historicoInvalido(carteiraId);
                nomes.putIfAbsent(id,nome(corretora));
            }
            for(var m:custodias){
                nomes.putIfAbsent(m.getCorretoraOrigemId(),nome(m.getCorretoraOrigem()));
                nomes.putIfAbsent(m.getCorretoraDestinoId(),nome(m.getCorretoraDestino()));
            }
            List<DiagnosticoCorretora> diagnosticos=estado.saldos().entrySet().stream().map(s->{
                var primeiro=estado.primeirosNegativos().get(s.getKey());
                return new DiagnosticoCorretora(s.getKey().corretoraId(),nomes.get(s.getKey().corretoraId()),s.getValue(),
                        s.getValue().signum()<0,primeiro!=null,primeiro==null?null:evidencia(primeiro));
            }).sorted(ORDEM_GRUPO).toList();
            List<QuantidadeCorretora> visiveis = diagnosticos.stream()
                    .filter(d -> d.quantidadeFinal().signum() != 0)
                    .map(d -> new QuantidadeCorretora(d.corretoraId(), d.nome(), d.quantidadeFinal())).toList();
            Reconciliacao reconciliacao = reconciliar(quantidade, diagnosticos);
            posicoes.add(new Posicao(acao.getId(), acao.getTicker(), acao.getMercado(), acao.getMoeda(),
                    quantidade, reconciliacao.quantidadeAtribuidaTotal(), reconciliacao.reconciliada(),
                    reconciliacao.diagnostico(), visiveis, diagnosticos));
        }
        posicoes.sort(Comparator.comparingInt((Posicao p) -> p.mercado() == Mercado.BRASIL ? 0 : 1)
                .thenComparing(Posicao::ticker).thenComparing(Posicao::acaoId));
        return new Distribuicao(carteiraId, posicoes);
    }

    static Reconciliacao reconciliar(BigDecimal quantidade, List<DiagnosticoCorretora> grupos) {
        BigDecimal total = grupos.stream().map(DiagnosticoCorretora::quantidadeFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean igual = quantidade.compareTo(total) == 0;
        return new Reconciliacao(total, igual, igual ? null
                : new DiagnosticoReconciliacao("DIVERGENCIA_QUANTIDADES"));
    }

    private static String nome(Corretora corretora) {
        if (corretora == null) return "Sem corretora informada";
        if (corretora.getNomeFantasia() != null && !corretora.getNomeFantasia().isBlank()) {
            return corretora.getNomeFantasia().trim();
        }
        if (corretora.getRazaoSocial() != null && !corretora.getRazaoSocial().isBlank()) {
            return corretora.getRazaoSocial().trim();
        }
        return "Corretora #" + corretora.getId();
    }

    private static ApiException historicoInvalido(Long carteiraId) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE,
                "Historico de Operacoes inconsistente", Map.of("carteiraId", carteiraId));
    }

    private static PrimeiroDeficit evidencia(MotorReplayQuantitativo.PrimeiroNegativo primeiro){
        var p=primeiro.posicao();
        Map<String,Object> posicao=p.financeira()?Map.of("ordemNoDia",p.ancoraFinanceira())
                :Map.of("ordemFinanceiraAncora",p.ancoraFinanceira(),"numerador",p.fracao().getNumerador().toString(),
                        "denominador",p.fracao().getDenominador().toString());
        return new PrimeiroDeficit(primeiro.identidade().dominio(),primeiro.identidade().idPersistido(),
                primeiro.natureza(),p.dataEfetiva(),posicao,primeiro.saldoAposEvento());
    }

    public record Distribuicao(Long carteiraId, List<Posicao> posicoes) {
        public Distribuicao { posicoes = List.copyOf(posicoes); }
    }
    public record Posicao(Long acaoId, String ticker, Mercado mercado, Moeda moeda,
                          BigDecimal quantidadeConsolidada, BigDecimal quantidadeAtribuidaTotal,
                          boolean reconciliada, DiagnosticoReconciliacao diagnosticoReconciliacao,
                          List<QuantidadeCorretora> corretoras, List<DiagnosticoCorretora> diagnosticosCorretoras) {
        public Posicao {
            corretoras = List.copyOf(corretoras);
            diagnosticosCorretoras = List.copyOf(diagnosticosCorretoras);
        }
    }
    public record QuantidadeCorretora(Long corretoraId, String nome, BigDecimal quantidadeLiquida) { }
    public record DiagnosticoCorretora(Long corretoraId, String nome, BigDecimal quantidadeFinal,
                                     boolean saldoNegativoAtual, boolean possuiDeficitHistorico,
                                     PrimeiroDeficit primeiroDeficit) { }
    public record PrimeiroDeficit(Dominio dominio,Long id,Natureza natureza,LocalDate dataEfetiva,
                                 Map<String,Object> posicaoCronologica,BigDecimal saldoAposEvento) {
        public PrimeiroDeficit {posicaoCronologica=Map.copyOf(posicaoCronologica);}
    }
    public record DiagnosticoReconciliacao(String codigo) { }
    record Reconciliacao(BigDecimal quantidadeAtribuidaTotal, boolean reconciliada,
                         DiagnosticoReconciliacao diagnostico) { }
}
