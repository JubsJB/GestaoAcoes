package com.projeto.services;

import com.projeto.dto.AcaoCreateRequest;
import com.projeto.dto.AcaoResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.integrations.cotacao.CotacaoCandidata;
import com.projeto.entities.ReferenciaTipoCotacao;
import com.projeto.integrations.cotacao.CotacaoProvider;
import com.projeto.mappers.AcaoMapper;
import com.projeto.repositories.AcaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import com.projeto.validation.TickerNormalizer;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AcaoService {

    private final TickerNormalizer tickerNormalizer;
    private final Map<Mercado, CotacaoProvider> providers;
    private final AcaoPersistenceService persistenceService;
    private final AplicacaoCotacaoService cotacaoPersistenceService;
    private final AcaoRepository repository;
    private final AcaoMapper mapper;
    private final Clock clock;

    @Value("${integration.alpha-vantage.quote-reuse-interval:15m}")
    private Duration quoteReuseInterval = Duration.ofMinutes(15);
    @Value("${integration.alpha-vantage.refresh-cooldown:15m}")
    private Duration refreshCooldown = Duration.ofMinutes(15);
    private final Map<String, RefreshAttempt> refreshAttempts = new ConcurrentHashMap<>();
    private final Object[] refreshLocks = java.util.stream.IntStream.range(0, 64)
            .mapToObj(i -> new Object()).toArray();

    @PostConstruct
    void validateRefreshIntervals() {
        if (quoteReuseInterval.isNegative() || quoteReuseInterval.isZero()
                || refreshCooldown.isNegative() || refreshCooldown.isZero()) {
            throw new IllegalArgumentException("Alpha Vantage refresh intervals must be positive");
        }
    }

    private record RefreshAttempt(Instant completedAt, ResultadoAtualizacaoCotacao result) { }


    public AcaoService(
            TickerNormalizer tickerNormalizer,
            List<CotacaoProvider> providers,
            AcaoPersistenceService persistenceService,
            AplicacaoCotacaoService cotacaoPersistenceService,
            AcaoRepository repository,
            AcaoMapper mapper,
            @Qualifier("cotacaoAcquisitionClock") Clock clock
    ) {
        this.tickerNormalizer = tickerNormalizer;
        this.providers = indexProviders(providers);
        this.persistenceService = persistenceService;
        this.cotacaoPersistenceService = cotacaoPersistenceService;
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    public AcaoResponse cadastrar(AcaoCreateRequest request) {
        if (request == null || request.getMercado() == null) {
            throw invalidRequest();
        }

        String requestedTicker = tickerNormalizer.normalizeAndValidate(request.getTicker());
        Mercado mercado = request.getMercado();
        persistenceService.ensureAvailable(requestedTicker, mercado);

        CotacaoProvider provider = providerFor(mercado);

        CotacaoCandidata candidata = provider.consultarCandidataCadastro(requestedTicker, clock);
        OffsetDateTime quoteTime = candidata.referenciaTipo() == ReferenciaTipoCotacao.INSTANTE
                ? candidata.referenciaInstante() : candidata.cotacaoObtidaEm();

        Acao acao = new Acao(
                candidata.ticker(),
                candidata.nomeEmpresa(),
                mercado,
                candidata.moeda(),
                candidata.cotacao(),
                quoteTime,
                candidata.referenciaTipo(),
                candidata.referenciaInstante(),
                candidata.referenciaData(),
                candidata.cotacaoObtidaEm()
        );
        return mapper.toResponse(persistenceService.saveUnique(acao));
    }

    @Transactional(readOnly = true)
    public List<AcaoResponse> listar() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcaoResponse buscarPorId(Long id) {
        Acao acao = repository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Ação não encontrada para o id: " + id
                ));

        return mapper.toResponse(acao);
    }

    @Transactional(readOnly = true)
    public AcaoResponse buscarPorTickerEMercado(String ticker, Mercado mercado) {
        String tickerNormalizado = tickerNormalizer.normalizeAndValidate(ticker);
        if (mercado == null) {
            throw invalidRequest();
        }

        Acao acao = repository.findByTickerAndMercado(tickerNormalizado, mercado)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Ação não encontrada para o ticker e mercado: "
                                + tickerNormalizado + " / " + mercado
                ));

        return mapper.toResponse(acao);
    }

    public AcaoResponse atualizarCotacao(Long id) {
        return atualizarCotacaoCompartilhada(id).respostaIndividual();
    }

    /** Única orquestração de refresh para individual e lote; sem transação envolvente. */
    public ResultadoAtualizacaoCotacao atualizarCotacaoCompartilhada(Long id) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Refresh não pode executar em transação envolvente");
        }
        Acao acao = repository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Ação não encontrada para o id: " + id
                ));

        if (acao.getMercado() != Mercado.EUA) return atualizarCotacaoPersistida(acao);
        // Local coordination includes the database reread and final persistence.
        synchronized (refreshLocks[Math.floorMod(acao.getTicker().hashCode(), refreshLocks.length)]) {
            Acao current = repository.findById(id).orElseThrow(() ->
                    new ObjectNotFoundException("Acao nao encontrada para o id: " + id));
            Instant now = clock.instant();
            RefreshAttempt previous = refreshAttempts.get(current.getTicker());
            if (previous != null && now.isBefore(previous.completedAt().plus(refreshCooldown))) {
                if (previous.result().falha() != null) {
                    return ResultadoAtualizacaoCotacao.de(current, RegraDecisaoCotacao.Resultado.FALHA,
                            "COOLDOWN_FALHA", ResultadoAtualizacaoCotacao.Origem.COOLDOWN,
                            ResultadoAtualizacaoCotacao.estado(current),
                            withPreservedQuote(previous.result().falha(), current));
                }
                return previous.result().emCooldown();
            }
            if (current.getCotacaoAtual() != null && current.getCotacaoAtual().signum() > 0
                    && referenciaConhecida(current)
                    && current.getCotacaoObtidaEm() != null
                    && !current.getCotacaoObtidaEm().toInstant().isAfter(now)
                    && now.isBefore(current.getCotacaoObtidaEm().toInstant().plus(quoteReuseInterval))) {
                return ResultadoAtualizacaoCotacao.de(current,
                        RegraDecisaoCotacao.Resultado.SEM_NOVA_COTACAO, "REUSO",
                        ResultadoAtualizacaoCotacao.Origem.REUSO,
                        ResultadoAtualizacaoCotacao.estado(current), null);
            }
            ResultadoAtualizacaoCotacao result = atualizarCotacaoPersistida(current);
            refreshAttempts.put(current.getTicker(), new RefreshAttempt(clock.instant(), result));
            return result;
        }
    }

    private ResultadoAtualizacaoCotacao atualizarCotacaoPersistida(Acao acao) {
        CotacaoCandidata candidata;
        try {
            candidata = providerFor(acao.getMercado()).consultarCandidataAtualizacao(
                    acao.getTicker(), acao.getNomeEmpresa(), acao.getMoeda().name(), clock);
        } catch (ApiException exception) {
            return ResultadoAtualizacaoCotacao.de(acao, RegraDecisaoCotacao.Resultado.FALHA,
                    exception.getCode(), ResultadoAtualizacaoCotacao.Origem.CONSULTA,
                    ResultadoAtualizacaoCotacao.estado(acao), withPreservedQuote(exception, acao));
        }
        // O proxy conclui a transação antes de fornecer o snapshot; integridade propaga sem retry.
        var aplicacao = cotacaoPersistenceService.aplicar(acao.getId(), candidata);
        var estado = aplicacao.estado();
        if (aplicacao.decisao().resultado() == RegraDecisaoCotacao.Resultado.FALHA) {
            String codigo = switch (aplicacao.decisao().motivo()) {
                case CONFLITO_NA_MESMA_REFERENCIA -> ErrorCodes.CONFLITO_NA_MESMA_REFERENCIA;
                case TIPO_REFERENCIA_INCOMPATIVEL -> ErrorCodes.TIPO_REFERENCIA_INCOMPATIVEL;
                case REFERENCIA_TEMPORAL_INVALIDA -> ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA;
                default -> throw new IllegalStateException("Motivo de falha temporal inválido");
            };
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("acaoId", acao.getId());
            details.put("cotacaoPreservada", true);
            details.put("ultimaCotacaoValida", estado.preco());
            details.put("dataHoraUltimaCotacao", estado.legado());
            return ResultadoAtualizacaoCotacao.de(acao, aplicacao.decisao().resultado(),
                    aplicacao.decisao().motivo().name(), ResultadoAtualizacaoCotacao.Origem.CONSULTA, estado,
                    new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, codigo,
                            "Conflito na referência temporal da cotação", details));
        }
        return ResultadoAtualizacaoCotacao.de(acao, aplicacao.decisao().resultado(),
                aplicacao.decisao().motivo().name(), ResultadoAtualizacaoCotacao.Origem.CONSULTA, estado, null);
    }

    private Map<Mercado, CotacaoProvider> indexProviders(List<CotacaoProvider> providerList) {
        Map<Mercado, CotacaoProvider> indexed = new EnumMap<>(Mercado.class);
        for (CotacaoProvider provider : providerList) {
            Mercado supportedMarket = provider.mercado();
            if (supportedMarket == null || indexed.putIfAbsent(supportedMarket, provider) != null) {
                throw new IllegalStateException("Configuração ambígua de provider de cotação");
            }
        }
        return Map.copyOf(indexed);
    }

    private CotacaoProvider providerFor(Mercado mercado) {
        CotacaoProvider provider = providers.get(mercado);
        if (provider == null) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,
                    "Serviço externo indisponível para o mercado informado"
            );
        }
        return provider;
    }

    private boolean referenciaConhecida(Acao acao) {
        return (acao.getReferenciaTipo() == ReferenciaTipoCotacao.DATA
                && acao.getReferenciaData() != null && acao.getReferenciaInstante() == null)
                || (acao.getReferenciaTipo() == ReferenciaTipoCotacao.INSTANTE
                && acao.getReferenciaInstante() != null && acao.getReferenciaData() == null);
    }

    private ApiException invalidRequest() {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.REQUEST_INVALIDO, "Dados da requisição inválidos");
    }

    private ApiException withPreservedQuote(ApiException exception, Acao acao) {
        Map<String, Object> details = new LinkedHashMap<>(exception.getDetails());
        details.put("acaoId", acao.getId());
        details.put("cotacaoPreservada", true);
        details.put("ultimaCotacaoValida", acao.getCotacaoAtual());
        details.put("dataHoraUltimaCotacao", acao.getDataHoraCotacao());
        return new ApiException(
                exception.getStatus(),
                exception.getCode(),
                exception.getMessage(),
                details
        );
    }
}
