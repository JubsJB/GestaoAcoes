package com.projeto.services;

import com.projeto.dto.OperacaoResponse;
import com.projeto.entities.*;
import com.projeto.mappers.OperacaoMapper;
import com.projeto.repositories.*;
import com.projeto.services.exceptions.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    public OperacaoPersistenceService(OperacaoRepository operacoes, CarteiraRepository carteiras,
            AcaoRepository acoes, CorretoraRepository corretoras, CalculadoraPosicao calculadora,
            OperacaoMapper mapper, ConstraintNameExtractor constraints) {
        this.operacoes=operacoes; this.carteiras=carteiras; this.acoes=acoes; this.corretoras=corretoras;
        this.calculadora=calculadora; this.mapper=mapper; this.constraints=constraints;
    }
    @Transactional
    public OperacaoResponse persistir(OperacaoPersistenceCommand command) {
        Carteira carteira = carteiras.findByIdForUpdate(command.carteiraId())
                .orElseThrow(() -> new ObjectNotFoundException("Carteira não encontrada para o id: " + command.carteiraId()));
        Acao acao = acoes.findByTickerAndMercado(command.ticker(), command.mercado())
                .orElseThrow(() -> new ObjectNotFoundException("Ação não encontrada para ticker " + command.ticker()
                        + " no mercado " + command.mercado()));
        Corretora corretora = command.corretoraId() == null ? null : corretoras.findById(command.corretoraId())
                .orElseThrow(() -> new ObjectNotFoundException("Corretora não encontrada para o id: " + command.corretoraId()));
        Integer max = operacoes.findMaxOrdemNoDia(carteira.getId(), acao.getId(), command.dataOperacao());
        if (max != null && max == Integer.MAX_VALUE)
            throw invalid("ordemNoDia", "Limite de operações no dia excedido");
        int order = max == null ? 1 : max + 1;
        BigDecimal total = calculateTotal(command.quantidade(), command.precoUnitario());
        Operacao candidate = new Operacao(carteira, acao, corretora, command.tipo(), command.quantidade(),
                command.precoUnitario(), command.dataOperacao(), order, total);
        List<Operacao> history = new ArrayList<>(operacoes
                .findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(carteira.getId(), acao.getId()));
        history.add(candidate); history.sort(ORDER);
        CalculadoraPosicao.ResultadoReplay replay = calculadora.validarQuantidade(history);
        if (!replay.valido()) {
            var f = replay.falha();
            Map<String,Object> details = new LinkedHashMap<>();
            details.put("carteiraId", carteira.getId()); details.put("ticker", acao.getTicker());
            details.put("mercado", acao.getMercado());
            if (f.quantidadeDisponivel()!=null) details.put("quantidadeDisponivel", f.quantidadeDisponivel());
            if (f.quantidadeSolicitada()!=null) details.put("quantidadeSolicitada", f.quantidadeSolicitada());
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.POSICAO_INSUFICIENTE,
                    "Posição insuficiente para a venda", details);
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
