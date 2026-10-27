package com.projeto.services;

import com.projeto.dto.OperacaoResponse;
import com.projeto.entities.*;
import com.projeto.mappers.OperacaoMapper;
import com.projeto.repositories.*;
import com.projeto.services.exceptions.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.IllegalTransactionStateException;
import com.projeto.validation.TickerNormalizer;
import java.time.Clock;
import static com.projeto.services.EventoQuantitativo.*;
import java.math.*;
import java.util.*;

@Service
public class OperacaoPersistenceService {
    private static final String ORDER_CONSTRAINT = "uk_operacao_carteira_acao_data_ordem";
    private static final Comparator<Operacao> ORDER = Comparator.comparing(Operacao::getDataOperacao)
            .thenComparing(Operacao::getOrdemNoDia);
    private final OperacaoRepository operacoes;
    private final CarteiraRepository carteiras;
    private final AcaoRepository acoes;
    private final CorretoraRepository corretoras;
    private final CalculadoraPosicao calculadora;
    private final OperacaoMapper mapper;
    private final ConstraintNameExtractor constraints;
    private final HistoricoQuantitativoCustodia historico;
    private final Clock clock;
    private final TickerNormalizer tickers;
    public OperacaoPersistenceService(OperacaoRepository operacoes, CarteiraRepository carteiras,
            AcaoRepository acoes, CorretoraRepository corretoras, CalculadoraPosicao calculadora,
            OperacaoMapper mapper, ConstraintNameExtractor constraints, HistoricoQuantitativoCustodia historico, Clock clock, TickerNormalizer tickers) {
        this.operacoes=operacoes; this.carteiras=carteiras; this.acoes=acoes; this.corretoras=corretoras;
        this.calculadora=calculadora; this.mapper=mapper; this.constraints=constraints; this.historico=historico; this.clock=clock; this.tickers=tickers;
    }
    @Transactional(propagation=Propagation.REQUIRED, isolation=Isolation.READ_COMMITTED, readOnly=false, rollbackFor=Exception.class)
    public OperacaoResponse persistir(OperacaoPersistenceCommand command) {
        Integer isolation = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        if (!TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || (isolation != null && isolation != java.sql.Connection.TRANSACTION_READ_COMMITTED))
            throw new IllegalTransactionStateException("Operação exige transação de escrita READ_COMMITTED compatível");
        if (command == null) throw OperacaoPersistenceCommand.invalida("request", "Operação obrigatória");
        command = command.validada(clock, tickers);
        // Referências somente após o lock; comando local final para lambdas de referência.
        var entrada = command;
        Carteira carteira = carteiras.findByIdForUpdate(command.carteiraId())
                .orElseThrow(() -> new ObjectNotFoundException("Carteira não encontrada para o id: " + entrada.carteiraId()));
        Acao acao = acoes.findByTickerAndMercado(command.ticker(), command.mercado())
                .orElseThrow(() -> new ObjectNotFoundException("Ação não encontrada para ticker " + entrada.ticker()
                        + " no mercado " + entrada.mercado()));
        Corretora corretora = command.corretoraId() == null ? null : corretoras.findById(command.corretoraId())
                .orElseThrow(() -> new ObjectNotFoundException("Corretora não encontrada para o id: " + entrada.corretoraId()));
        Integer max = operacoes.findMaxOrdemNoDia(carteira.getId(), acao.getId(), command.dataOperacao());
        if (max != null && max == Integer.MAX_VALUE)
            throw invalid("ordemNoDia", "Limite de operações no dia excedido");
        int order = max == null ? 1 : max + 1;
        BigDecimal total = calculateTotal(command.quantidade(), command.precoUnitario());
        Operacao candidate = new Operacao(carteira, acao, corretora, command.tipo(), command.quantidade(),
                command.precoUnitario(), command.dataOperacao(), order, total);
        var carregado = historico.carregar(carteira.getId(), acao.getId());
        List<Operacao> history = new ArrayList<>(carregado.financeiros());
        history.add(candidate); history.sort(ORDER);
        CalculadoraPosicao.ResultadoReplay replay = calculadora.validarQuantidade(history);
        var eventos = carregado.eventos();
        var id = Identidade.temporaria(Dominio.FINANCEIRO, UUID.randomUUID());
        var evento = new EventoQuantitativo(id, carteira.getId(), acao.getId(), acao.getMercado(),
                command.tipo() == TipoOperacao.COMPRA ? Natureza.COMPRA : Natureza.VENDA, command.quantidade(),
                PosicaoCronologicaQuantitativa.financeira(command.dataOperacao(), order), new Grupo(command.corretoraId()), null, false);
        var posteriores = new ArrayList<>(eventos); posteriores.add(evento);
        posteriores.sort(Comparator.comparing(EventoQuantitativo::posicao));
        var motor = new MotorReplayQuantitativo();
        var validacao = new ValidadorHistoricoQuantitativo().validar(motor.reproduzir(eventos), motor.reproduzir(posteriores),
                new ValidadorHistoricoQuantitativo.Intencao(command.tipo() == TipoOperacao.COMPRA
                        ? ValidadorHistoricoQuantitativo.TipoIntencao.COMPRA : ValidadorHistoricoQuantitativo.TipoIntencao.VENDA, id));
        if (!replay.valido()) {
            var f = replay.falha();
            Map<String,Object> details = new LinkedHashMap<>();
            details.put("carteiraId", carteira.getId()); details.put("ticker", acao.getTicker());
            details.put("mercado", acao.getMercado());
            if (f.quantidadeDisponivel()!=null) details.put("quantidadeDisponivel", f.quantidadeDisponivel());
            if (f.quantidadeSolicitada()!=null) details.put("quantidadeSolicitada", f.quantidadeSolicitada());
            if (!validacao.valido()) details.putAll(DetalhesConflitoQuantitativo.montar(carteira.getId(), acao.getId(),
                    evento.grupoOrigem(), command.quantidade(), ErrorCodes.POSICAO_INSUFICIENTE, validacao.conflito(), posteriores));
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.POSICAO_INSUFICIENTE,
                    "Posição insuficiente para a venda", details);
        }
        if (!validacao.valido()) {
            var causa = validacao.conflito();
            throw new ApiException(HttpStatus.CONFLICT, causa.codigo().name(), "Operação incompatível com o histórico quantitativo",
                    DetalhesConflitoQuantitativo.montar(carteira.getId(), acao.getId(), evento.grupoOrigem(), command.quantidade(),
                            causa.codigo().name(), causa, posteriores));
        }
        try { return mapper.toResponse(operacoes.saveAndFlush(candidate)); }
        catch (DataIntegrityViolationException e) {
            if (constraints.extractConstraintName(e).filter(ORDER_CONSTRAINT::equalsIgnoreCase).isPresent())
                throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.INTEGRIDADE_DADOS_VIOLADA,
                        "Conflito de integridade ao gerar a ordem da operação");
            throw e;
        }
    }
    private BigDecimal calculateTotal(BigDecimal quantity, BigDecimal price) {
        BigDecimal total = quantity.multiply(price);
        if (total.scale()>12 || total.precision()>38) throw invalid("valorTotal", "Valor total excede precisão máxima");
        return total.setScale(12, RoundingMode.UNNECESSARY);
    }
    private ApiException invalid(String field,String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.REQUEST_INVALIDO,
                "Dados da requisição inválidos", Map.of(field,message));
    }
}
