package com.projeto.services;

import com.projeto.entities.*;
import com.projeto.services.exceptions.ObjectNotFoundException;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ConstraintNameExtractor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import com.projeto.repositories.*;
import com.projeto.validation.TickerNormalizer;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.projeto.services.EventoQuantitativo.*;

@Service
public class TransferenciaCustodiaPersistenceService {
    private static final String ESTORNO_UNIQUE = "uk_estorno_movimentacao_custodia_original";
    private final CarteiraRepository carteiras;
    private final AcaoRepository acoes;
    private final CorretoraRepository corretoras;
    private final OperacaoRepository operacoes;
    private final MovimentacaoCustodiaRepository movimentos;
    private final EstornoMovimentacaoCustodiaRepository estornos;
    private final Clock clock;
    private final TickerNormalizer tickers;
    public TransferenciaCustodiaPersistenceService(CarteiraRepository carteiras,AcaoRepository acoes,
            CorretoraRepository corretoras,OperacaoRepository operacoes,MovimentacaoCustodiaRepository movimentos,
            EstornoMovimentacaoCustodiaRepository estornos,Clock clock,TickerNormalizer tickers) {
        this.carteiras = carteiras;
        this.acoes = acoes;
        this.corretoras = corretoras;
        this.operacoes = operacoes;
        this.movimentos = movimentos;
        this.estornos = estornos;
        this.clock = clock;
        this.tickers = tickers;
    }
    @Transactional(propagation=Propagation.REQUIRED,isolation=Isolation.READ_COMMITTED,readOnly=false,rollbackFor=Exception.class)
    public MovimentacaoCustodia persistir(TransferenciaCustodiaCommand command) {
        exigirTransacaoCompativel();
        if (command == null) throw TransferenciaCustodiaCommand.invalida("request", "Transferência obrigatória");
        var c = command.validada(tickers, clock);
        return registrar(new DadosRegistro(c.carteiraId(), c.ticker(), c.mercado(), c.corretoraOrigemId(),
                c.corretoraDestinoId(), c.quantidade(), c.dataEfetiva(), NaturezaMovimentacaoCustodia.TRANSFERENCIA));
    }

    @Transactional(propagation=Propagation.REQUIRED,isolation=Isolation.READ_COMMITTED,readOnly=false,rollbackFor=Exception.class)
    public MovimentacaoCustodia persistirAtribuicao(AtribuicaoCustodiaCommand command) {
        exigirTransacaoCompativel();
        if (command == null) throw ValidacaoEntradaCustodia.invalida("request", "Atribuição obrigatória");
        var c = command.validada(tickers, clock);
        return registrar(new DadosRegistro(c.carteiraId(), c.ticker(), c.mercado(), null,
                c.corretoraDestinoId(), c.quantidade(), c.dataEfetiva(), NaturezaMovimentacaoCustodia.ATRIBUICAO));
    }

    @Transactional(propagation=Propagation.REQUIRED,isolation=Isolation.READ_COMMITTED,readOnly=false,rollbackFor=Exception.class)
    public EstornoMovimentacaoCustodia persistirEstorno(EstornoCustodiaCommand command) {
        exigirTransacaoCompativel();
        if (command == null) throw ValidacaoEntradaCustodia.invalida("request", "Estorno obrigatório");
        var c = command.validada();
        carteiras.findByIdForUpdate(c.carteiraId())
                .orElseThrow(() -> new ObjectNotFoundException("Carteira não encontrada"));
        var original = movimentos.buscarPorCarteiraEId(c.carteiraId(), c.movimentacaoOriginalId())
                .orElseThrow(() -> new ObjectNotFoundException("Movimentação não encontrada"));
        var vinculos = estornos.listarPorCarteiraEOriginais(c.carteiraId(), List.of(original.getId()));
        if (!vinculos.isEmpty()) throw estornoDuplicado(original);
        var eventos = carregarEventos(c.carteiraId(), original.getAcaoId());
        var motor = new MotorReplayQuantitativo();
        var validador = new ValidadorHistoricoQuantitativo();
        var identidade = Identidade.persistida(Dominio.CUSTODIA, original.getId());
        var intencao = new ValidadorHistoricoQuantitativo.Intencao(
                ValidadorHistoricoQuantitativo.TipoIntencao.ESTORNO, identidade);
        var antes = motor.reproduzir(eventos);
        var precondicao = validador.verificarPrecondicoes(antes, intencao);
        if (!precondicao.valido()) throw conflitoEstorno(original, precondicao.conflito(), eventos);
        // Simulação escalar: nenhuma candidata JPA é apresentada como vínculo confirmado.
        var neutralizados = eventos.stream().map(e -> e.identidade().equals(identidade)
                ? new EventoQuantitativo(e.identidade(), e.carteiraId(), e.acaoId(), e.mercado(), e.natureza(),
                        e.quantidade(), e.posicao(), e.grupoOrigem(), e.grupoDestino(), true)
                : e).toList();
        var resultado = validador.validar(antes, motor.reproduzir(neutralizados), intencao);
        if (!resultado.valido()) throw conflitoEstorno(original, resultado.conflito(), eventos);
        try {
            return estornos.saveAndFlush(new EstornoMovimentacaoCustodia(null, original, c.motivo(), clock));
        } catch (DataIntegrityViolationException falha) {
            // Classificação estrita. A transação falhou: não consultar nem tentar recuperar aqui.
            boolean duplicado = new ConstraintNameExtractor()
                    .extractConstraintName(falha)
                    .filter(ESTORNO_UNIQUE::equals).isPresent();
            if (duplicado) throw estornoDuplicado(original);
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.INTEGRIDADE_DADOS_VIOLADA,
                    "A operação viola uma regra de integridade dos dados.");
        }
    }

    private ApiException estornoDuplicado(MovimentacaoCustodia original) {
        var fracao = original.getPosicaoRacional();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("carteiraId", original.getCarteiraId());
        details.put("acaoId", original.getAcaoId());
        details.put("origem", grupo(new Grupo(original.getCorretoraOrigemId())));
        details.put("motivo", ErrorCodes.ESTORNO_DUPLICADO);
        details.put("evento", Map.of("dominio", Dominio.CUSTODIA.name(), "id", original.getId(),
                "natureza", original.getNatureza().name(), "dataEfetiva", original.getDataEfetiva(),
                "posicaoCronologica", Map.of("ordemFinanceiraAncora", original.getOrdemFinanceiraAncora(),
                        "numerador", fracao.getNumeradorTexto(), "denominador", fracao.getDenominadorTexto())));
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.ESTORNO_DUPLICADO,
                "Movimentação já estornada", details);
    }

    private void exigirTransacaoCompativel() {
        // REQUIRED herda as características do chamador: rejeitar antes de qualquer consulta.
        Integer isolamento = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || (isolamento != null && isolamento != java.sql.Connection.TRANSACTION_READ_COMMITTED)) {
            throw new IllegalTransactionStateException("Custódia exige transação de escrita READ_COMMITTED compatível");
        }
    }

    /** Dados internos: direção e natureza são fixadas pelas entradas transacionais, nunca pelo cliente. */
    private record DadosRegistro(Long carteiraId, String ticker, Mercado mercado, Long corretoraOrigemId,
            Long corretoraDestinoId, java.math.BigDecimal quantidade, java.time.LocalDate dataEfetiva,
            NaturezaMovimentacaoCustodia natureza) { }

    private MovimentacaoCustodia registrar(DadosRegistro c) {
        var carteira=carteiras.findByIdForUpdate(c.carteiraId()).orElseThrow(()->new ObjectNotFoundException("Carteira não encontrada"));
        var acao=acoes.findByTickerAndMercado(c.ticker(),c.mercado()).orElseThrow(()->new ObjectNotFoundException("Ação não encontrada"));
        var origem=c.corretoraOrigemId()==null ? null : corretoras.findById(c.corretoraOrigemId())
                .orElseThrow(()->new ObjectNotFoundException("Corretora de origem não encontrada"));
        var destino=corretoras.findById(c.corretoraDestinoId()).orElseThrow(()->new ObjectNotFoundException("Corretora de destino não encontrada"));
        var eventos = carregarEventos(carteira.getId(), acao.getId());
        var identidade=Identidade.temporaria(Dominio.CUSTODIA,UUID.randomUUID());
        var resultado=new BuscadorPosicaoCustodia().buscar(eventos,
                new ValidadorHistoricoQuantitativo.Intencao(c.natureza()==NaturezaMovimentacaoCustodia.TRANSFERENCIA
                        ? ValidadorHistoricoQuantitativo.TipoIntencao.TRANSFERENCIA
                        : ValidadorHistoricoQuantitativo.TipoIntencao.ATRIBUICAO,identidade),
                new BuscadorPosicaoCustodia.DadosCandidata(carteira.getId(),acao.getId(),acao.getMercado(),c.dataEfetiva(),c.quantidade(),
                        new Grupo(c.corretoraOrigemId()),Grupo.corretora(destino.getId())));
        if(!resultado.encontrada())throw conflito(c,acao,resultado,eventos);
        var movimento=new MovimentacaoCustodia(null,carteira,acao,c.natureza(),origem,destino,
                c.quantidade(),c.dataEfetiva(),resultado.candidata().posicao().ancoraFinanceira(),resultado.posicaoPersistivel(),clock);
        return movimentos.saveAndFlush(movimento);
    }

    private List<EventoQuantitativo> carregarEventos(Long carteiraId, Long acaoId) {
        return new HistoricoQuantitativoCustodia(operacoes, movimentos, estornos).carregar(carteiraId, acaoId).eventos();
    }
    private ApiException conflito(DadosRegistro c,Acao acao,BuscadorPosicaoCustodia.ResultadoBusca resultado,
            List<EventoQuantitativo> eventos) {
        var diagnostico=resultado.diagnostico();var causa=diagnostico.causa();
        String codigo=switch(diagnostico.codigo()) {
            case POSICAO_INSUFICIENTE->ErrorCodes.POSICAO_INSUFICIENTE;
            case SALDO_ORIGEM_INSUFICIENTE->ErrorCodes.SALDO_ORIGEM_INSUFICIENTE;
            case POSICAO_CUSTODIA_INDISPONIVEL->ErrorCodes.POSICAO_CUSTODIA_INDISPONIVEL;
            case LIMITE_POSICAO_CUSTODIA->ErrorCodes.LIMITE_POSICAO_CUSTODIA;
        };
        var details = detalhesConflito(c.carteiraId(), acao.getId(), new Grupo(c.corretoraOrigemId()),
                c.quantidade(), causa==null ? diagnostico.codigo().name() : causa.codigo().name(), causa, eventos);
        return new ApiException(HttpStatus.CONFLICT,codigo,"Movimentação incompatível com o histórico quantitativo",details);
    }
    private ApiException conflitoEstorno(MovimentacaoCustodia original,
            ValidadorHistoricoQuantitativo.Conflito causa, List<EventoQuantitativo> eventos) {
        String codigo = switch (causa.codigo()) {
            case ESTORNO_DUPLICADO -> ErrorCodes.ESTORNO_DUPLICADO;
            case POSICAO_INSUFICIENTE -> ErrorCodes.POSICAO_INSUFICIENTE;
            case MOVIMENTACAO_CUSTODIA_INVALIDADA -> ErrorCodes.MOVIMENTACAO_CUSTODIA_INVALIDADA;
            case CONFLITO_HISTORICO_QUANTITATIVO -> ErrorCodes.CONFLITO_HISTORICO_QUANTITATIVO;
            default -> throw new IllegalStateException("Diagnóstico incompatível com estorno");
        };
        return new ApiException(HttpStatus.CONFLICT, codigo, "Estorno incompatível com o histórico quantitativo",
                detalhesConflito(original.getCarteiraId(), original.getAcaoId(), new Grupo(original.getCorretoraOrigemId()),
                        null, causa.codigo().name(), causa, eventos));
    }

    private Map<String, Object> detalhesConflito(Long carteiraId, Long acaoId, Grupo origem,
            java.math.BigDecimal quantidadeSolicitada, String motivo,
            ValidadorHistoricoQuantitativo.Conflito causa, List<EventoQuantitativo> eventos) {
        return DetalhesConflitoQuantitativo.montar(carteiraId, acaoId, origem, quantidadeSolicitada, motivo, causa, eventos);
    }
    private Map<String,Object> grupo(Grupo grupo) { return DetalhesConflitoQuantitativo.grupo(grupo); }
}
