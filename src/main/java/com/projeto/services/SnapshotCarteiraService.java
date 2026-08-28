package com.projeto.services;

import com.projeto.dto.PosicaoResponse;
import com.projeto.dto.SnapshotCarteiraResponse;
import com.projeto.entities.Carteira;
import com.projeto.entities.SnapshotCarteira;
import com.projeto.entities.SnapshotCarteiraMoeda;
import com.projeto.mappers.SnapshotCarteiraMapper;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.SnapshotCarteiraMoedaRepository;
import com.projeto.repositories.SnapshotCarteiraRepository;
import com.projeto.services.AgregadorPosicoesPorMoeda.FalhaAgregacaoException;
import com.projeto.services.AgregadorPosicoesPorMoeda.TotaisPorMoeda;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import com.projeto.services.exceptions.ConstraintNameExtractor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SnapshotCarteiraService {

    private static final String UNIQUE_TEMPORAL = "uk_snapshot_carteira_carteira_data_hora";

    private final Clock clock;
    private final CarteiraRepository carteiraRepository;
    private final PosicaoService posicaoService;
    private final AgregadorPosicoesPorMoeda agregador;
    private final SnapshotCarteiraRepository snapshotRepository;
    private final SnapshotCarteiraMoedaRepository componenteRepository;
    private final SnapshotCarteiraMapper mapper;
    private final ConstraintNameExtractor constraintNameExtractor;

    public SnapshotCarteiraService(
            Clock clock,
            CarteiraRepository carteiraRepository,
            PosicaoService posicaoService,
            AgregadorPosicoesPorMoeda agregador,
            SnapshotCarteiraRepository snapshotRepository,
            SnapshotCarteiraMoedaRepository componenteRepository,
            SnapshotCarteiraMapper mapper,
            ConstraintNameExtractor constraintNameExtractor
    ) {
        this.clock = clock;
        this.carteiraRepository = carteiraRepository;
        this.posicaoService = posicaoService;
        this.agregador = agregador;
        this.snapshotRepository = snapshotRepository;
        this.componenteRepository = componenteRepository;
        this.mapper = mapper;
        this.constraintNameExtractor = constraintNameExtractor;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public SnapshotCarteiraResponse criar(Long carteiraId) {
        OffsetDateTime dataHoraSnapshot = OffsetDateTime.now(clock)
                .withOffsetSameInstant(ZoneOffset.UTC);
        List<PosicaoResponse> posicoes = posicaoService.listarPorCarteira(carteiraId);
        Carteira carteira = carteiraRepository.getReferenceById(carteiraId);

        List<TotaisPorMoeda> totais;
        try {
            totais = agregador.agregar(posicoes);
        } catch (FalhaAgregacaoException exception) {
            throw falhaCalculo(carteiraId, exception);
        }

        SnapshotCarteira snapshot;
        try {
            snapshot = snapshotRepository.saveAndFlush(
                    new SnapshotCarteira(carteira, dataHoraSnapshot)
            );
        } catch (DataIntegrityViolationException exception) {
            if (constraintNameExtractor.extractConstraintName(exception)
                    .filter(UNIQUE_TEMPORAL::equalsIgnoreCase)
                    .isPresent()) {
                throw snapshotDuplicado(carteiraId, dataHoraSnapshot);
            }
            throw exception;
        }

        List<SnapshotCarteiraMoeda> componentes = new ArrayList<>(totais.size());
        for (TotaisPorMoeda total : totais) {
            componentes.add(new SnapshotCarteiraMoeda(
                    snapshot,
                    total.moeda(),
                    total.patrimonioAtual()
            ));
        }
        if (!componentes.isEmpty()) {
            componentes = componenteRepository.saveAllAndFlush(componentes);
        }
        return mapper.toResponse(snapshot, componentes);
    }

    private ApiException snapshotDuplicado(Long carteiraId, OffsetDateTime dataHoraSnapshot) {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCodes.SNAPSHOT_CARTEIRA_DUPLICADO,
                "Já existe snapshot da Carteira para este instante",
                Map.of("carteiraId", carteiraId, "dataHoraSnapshot", dataHoraSnapshot)
        );
    }

    private ApiException falhaCalculo(Long carteiraId, FalhaAgregacaoException exception) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("carteiraId", carteiraId);
        detalhes.put("moeda", exception.moeda());
        detalhes.put("motivo", exception.getMessage());
        return new ApiException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO,
                "Cálculo do snapshot excede a precisão aprovada",
                detalhes
        );
    }
}
