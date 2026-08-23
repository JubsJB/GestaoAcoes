package com.projeto.services;

import com.projeto.dto.OperacaoCreateRequest;
import com.projeto.dto.OperacaoResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Corretora;
import com.projeto.entities.Mercado;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import com.projeto.mappers.OperacaoMapper;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.CorretoraRepository;
import com.projeto.repositories.OperacaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import com.projeto.validation.TickerNormalizer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OperacaoService {

    private static final int OPERAND_PRECISION = 19;
    private static final int OPERAND_SCALE = 6;
    private static final int TOTAL_PRECISION = 38;
    private static final int TOTAL_SCALE = 12;
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final ZoneId USA_ZONE = ZoneId.of("America/New_York");
    private static final String ORDER_CONSTRAINT = "UK_OPERACAO_CARTEIRA_ACAO_DATA_ORDEM";
    private static final Sort QUERY_ORDER = Sort.by(
            Sort.Order.asc("dataOperacao"),
            Sort.Order.asc("ordemNoDia"),
            Sort.Order.asc("id")
    );

    private static final Comparator<Operacao> CHRONOLOGICAL_ORDER = Comparator
            .comparing(Operacao::getDataOperacao)
            .thenComparing(Operacao::getOrdemNoDia);

    private final OperacaoRepository operacaoRepository;
    private final CarteiraRepository carteiraRepository;
    private final AcaoRepository acaoRepository;
    private final CorretoraRepository corretoraRepository;
    private final TickerNormalizer tickerNormalizer;
    private final OperacaoMapper mapper;
    private final Clock clock;

    public OperacaoService(
            OperacaoRepository operacaoRepository,
            CarteiraRepository carteiraRepository,
            AcaoRepository acaoRepository,
            CorretoraRepository corretoraRepository,
            TickerNormalizer tickerNormalizer,
            OperacaoMapper mapper,
            Clock clock
    ) {
        this.operacaoRepository = operacaoRepository;
        this.carteiraRepository = carteiraRepository;
        this.acaoRepository = acaoRepository;
        this.corretoraRepository = corretoraRepository;
        this.tickerNormalizer = tickerNormalizer;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public OperacaoResponse cadastrar(OperacaoCreateRequest request) {
        validateRequiredRequest(request);

        Carteira carteira = carteiraRepository.findByIdForUpdate(request.getCarteiraId())
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Carteira não encontrada para o id: " + request.getCarteiraId()
                ));

        String normalizedTicker = tickerNormalizer.normalizeAndValidate(request.getTicker());
        Acao acao = acaoRepository.findByTickerAndMercado(normalizedTicker, request.getMercado())
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Ação não encontrada para ticker " + normalizedTicker
                                + " no mercado " + request.getMercado()
                ));

        Corretora corretora = findBroker(request.getCorretoraId());
        BigDecimal quantidade = validateOperand(request.getQuantidade(), "quantidade", "Quantidade");
        validateQuantityForMarket(quantidade, acao.getMercado());
        BigDecimal precoUnitario = validateOperand(
                request.getPrecoUnitario(),
                "precoUnitario",
                "Preço unitário"
        );
        validateOperationDate(request.getDataOperacao(), acao.getMercado());
        validateOrder(request.getOrdemNoDia());

        BigDecimal valorTotal = calculateTotal(quantidade, precoUnitario);
        ensureUniqueOrder(carteira.getId(), acao.getId(), request.getDataOperacao(), request.getOrdemNoDia());

        Operacao candidate = new Operacao(
                carteira,
                acao,
                corretora,
                request.getTipo(),
                quantidade,
                precoUnitario,
                request.getDataOperacao(),
                request.getOrdemNoDia(),
                valorTotal
        );

        List<Operacao> history = operacaoRepository
                .findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(
                        carteira.getId(),
                        acao.getId()
                );
        validateReplay(history, candidate);

        try {
            return mapper.toResponse(operacaoRepository.saveAndFlush(candidate));
        } catch (DataIntegrityViolationException exception) {
            if (isOrderConstraintViolation(exception)) {
                throw duplicateOrder(carteira.getId(), acao, request.getDataOperacao(), request.getOrdemNoDia());
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<OperacaoResponse> listar() {
        return operacaoRepository.findAll(QUERY_ORDER)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OperacaoResponse buscarPorId(Long id) {
        Operacao operacao = operacaoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Operação não encontrada para o id: " + id
                ));

        return mapper.toResponse(operacao);
    }

    @Transactional(readOnly = true)
    public List<OperacaoResponse> listarPorCarteira(Long carteiraId) {
        carteiraRepository.findById(carteiraId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Carteira não encontrada para o id: " + carteiraId
                ));

        return operacaoRepository
                .findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(carteiraId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    private void validateRequiredRequest(OperacaoCreateRequest request) {
        if (request == null) {
            throw invalidRequest("request", "Corpo da requisição é obrigatório");
        }
        if (request.getCarteiraId() == null) {
            throw invalidRequest("carteiraId", "Carteira é obrigatória");
        }
        if (request.getMercado() == null) {
            throw invalidRequest("mercado", "Mercado é obrigatório");
        }
        if (request.getTipo() == null) {
            throw invalidRequest("tipo", "Tipo é obrigatório");
        }
    }

    private Corretora findBroker(Long corretoraId) {
        if (corretoraId == null) {
            return null;
        }
        return corretoraRepository.findById(corretoraId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Corretora não encontrada para o id: " + corretoraId
                ));
    }

    private BigDecimal validateOperand(BigDecimal value, String field, String label) {
        if (value == null) {
            throw invalidRequest(field, label + " é obrigatório");
        }
        if (value.signum() <= 0) {
            throw invalidRequest(field, label + " deve ser maior que zero");
        }
        if (value.scale() > OPERAND_SCALE) {
            throw invalidRequest(field, label + " deve possuir no máximo 6 casas decimais");
        }

        BigDecimal normalized;
        try {
            normalized = value.setScale(OPERAND_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw invalidRequest(field, label + " não pode ser arredondado ou truncado");
        }
        if (normalized.precision() > OPERAND_PRECISION) {
            throw invalidRequest(field, label + " excede a precisão máxima 19");
        }
        return normalized;
    }

    private void validateQuantityForMarket(BigDecimal quantidade, Mercado mercado) {
        if (mercado == Mercado.BRASIL && quantidade.stripTrailingZeros().scale() > 0) {
            throw invalidRequest(
                    "quantidade",
                    "Quantidade deve ser matematicamente inteira para o mercado BRASIL"
            );
        }
    }

    private BigDecimal calculateTotal(BigDecimal quantidade, BigDecimal precoUnitario) {
        BigDecimal total = quantidade.multiply(precoUnitario);
        if (total.scale() > TOTAL_SCALE || total.precision() > TOTAL_PRECISION) {
            throw invalidRequest("valorTotal", "Valor total excede a precisão máxima 38 e escala máxima 12");
        }
        try {
            return total.setScale(TOTAL_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw invalidRequest("valorTotal", "Valor total não pode ser arredondado ou truncado");
        }
    }

    private void validateOperationDate(LocalDate date, Mercado market) {
        if (date == null) {
            throw invalidRequest("dataOperacao", "Data da operação é obrigatória");
        }
        ZoneId marketZone = market == Mercado.BRASIL ? BRAZIL_ZONE : USA_ZONE;
        LocalDate currentMarketDate = LocalDate.now(clock.withZone(marketZone));
        if (date.isAfter(currentMarketDate)) {
            throw invalidRequest("dataOperacao", "Data da operação não pode ser futura");
        }
    }

    private void validateOrder(Integer order) {
        if (order == null) {
            throw invalidRequest("ordemNoDia", "Ordem no dia é obrigatória");
        }
        if (order <= 0) {
            throw invalidRequest("ordemNoDia", "Ordem no dia deve ser maior que zero");
        }
    }

    private void ensureUniqueOrder(Long carteiraId, Long acaoId, LocalDate date, Integer order) {
        if (operacaoRepository.existsByCarteiraIdAndAcaoIdAndDataOperacaoAndOrdemNoDia(
                carteiraId,
                acaoId,
                date,
                order
        )) {
            throw duplicateOrder(carteiraId, acaoId, date, order);
        }
    }

    private void validateReplay(List<Operacao> history, Operacao candidate) {
        List<Operacao> chronological = new ArrayList<>(history.size() + 1);
        chronological.addAll(history);
        chronological.add(candidate);
        chronological.sort(CHRONOLOGICAL_ORDER);

        BigDecimal balance = BigDecimal.ZERO;
        for (Operacao operation : chronological) {
            if (operation.getTipo() == TipoOperacao.COMPRA) {
                balance = balance.add(operation.getQuantidade());
                continue;
            }

            BigDecimal available = balance;
            balance = balance.subtract(operation.getQuantidade());
            if (balance.signum() < 0) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        ErrorCodes.POSICAO_INSUFICIENTE,
                        "Posição insuficiente para a venda",
                        Map.of(
                                "carteiraId", candidate.getCarteira().getId(),
                                "ticker", candidate.getAcao().getTicker(),
                                "mercado", candidate.getAcao().getMercado(),
                                "quantidadeDisponivel", available,
                                "quantidadeSolicitada", operation.getQuantidade()
                        )
                );
            }
        }
    }

    private ApiException duplicateOrder(Long carteiraId, Acao acao, LocalDate date, Integer order) {
        return duplicateOrder(carteiraId, acao.getId(), date, order);
    }

    private ApiException duplicateOrder(Long carteiraId, Long acaoId, LocalDate date, Integer order) {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCodes.ORDEM_OPERACAO_DUPLICADA,
                "Já existe uma operação nessa ordem cronológica",
                Map.of(
                        "carteiraId", carteiraId,
                        "acaoId", acaoId,
                        "dataOperacao", date,
                        "ordemNoDia", order
                )
        );
    }

    private boolean isOrderConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toUpperCase(Locale.ROOT).contains(ORDER_CONSTRAINT)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ApiException invalidRequest(String field, String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.REQUEST_INVALIDO,
                "Dados da requisição inválidos",
                Map.of(field, message)
        );
    }
}
